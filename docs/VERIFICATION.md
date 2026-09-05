# Release verification checklist

This checklist is the reproducible release gate for the narrow Northstar MVP.
Run it only with designated Salesforce test data. It combines deterministic
automated proof with one deliberate live happy-path check; failure cases do not
mutate Salesforce merely to provoke an error.

## 1. Preflight and secret safety

- [ ] Use Java 21 and a working Docker daemon.
- [ ] Copy `.env.example` to ignored `.env` and replace every placeholder.
- [ ] Confirm `git status --short` does not list `.env`, build output, logs, or
  copied Kafka/customer payloads.
- [ ] Confirm `.env` has never been tracked: `git log --all -- .env` should
  produce no commits.
- [ ] Search tracked files and Git history for the actual local client ID,
  client secret, token URL, and any other private environment value. Perform
  comparisons without printing the values to terminal logs.
- [ ] Search tracked files for local absolute paths, private Salesforce hosts,
  authorization headers, access tokens, and real customer payloads.

Do not paste secret-search commands containing credentials into documentation
or shell history. Read each value from the local environment and report only
whether a match exists.

## 2. Automated release build

From the repository root:

```bash
./mvnw verify
```

This is the release command. It enforces Spotless formatting, executes both
modules' default test suites, and packages both executable Spring Boot JARs.
It must finish with reactor `BUILD SUCCESS` without live Salesforce access.

The suite provides deterministic evidence for:

- missing-business-ID rejection before event creation or publication;
- corrected valid-customer acceptance through the normal path;
- Kafka publication, retry recovery, validation DLT, and exhausted-retry DLT;
- exact-event deduplication and repeated-customer create-or-update behavior;
- Flyway-managed PostgreSQL schemas, audit transitions, safe logs, and health.

The exact outcome-to-test mapping is in `docs/OBSERVABILITY.md`.

## 3. Clean local infrastructure

For an isolated release check, use a temporary Compose project name. This
avoids deleting or reusing the developer's normal PostgreSQL volume:

```bash
set -a
source .env
set +a

docker compose --project-name northstar-release-verify up -d broker postgres
docker compose --project-name northstar-release-verify ps
```

Wait for PostgreSQL to report healthy and for Kafka to accept an admin request:

```bash
docker compose --project-name northstar-release-verify exec -T postgres \
  pg_isready -U "$PG_USERNAME" -d "$PG_DATABASE"

docker compose --project-name northstar-release-verify exec -T broker \
  /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
```

Start the packaged JARs in separate shells so this step verifies the artifacts,
not only the Maven development launcher:

```bash
# terminal 1: mock ERP
set -a; source .env; set +a
CUSTOMER_SYNC_LISTENER_ENABLED=true SERVER_PORT=8081 \
  java -jar mock-erp-service/target/mock-erp-service-0.0.1-SNAPSHOT.jar
```

```bash
# terminal 2: integration service
set -a; source .env; set +a
java -jar integration-service/target/integration-service-0.0.1-SNAPSHOT.jar
```

Both applications must apply their Flyway migrations successfully. Verify
readiness after Kafka and PostgreSQL are available:

```bash
curl -sS http://localhost:8080/actuator/health/readiness
curl -sS http://localhost:8081/actuator/health/readiness
```

Expected response from each service: `{"status":"UP"}`.

## 4. Live happy path and publication audit

Trigger the designated Account once:

```bash
curl -i -X POST \
  "http://localhost:8080/api/sync/account/$SALESFORCE_TEST_ACCOUNT_ID"
```

Verify HTTP `202`, status `ACCEPTED`, and non-empty `eventId` and
`correlationId`. Verify the response contains no token, client secret,
authorization header, or Salesforce instance URL.

Query the returned correlation ID:

```bash
CORRELATION_ID=replace-with-returned-correlation-id
curl -sS "http://localhost:8080/api/sync/$CORRELATION_ID"
```

Expected integration-owned state: `PUBLISHED`. This means the broker
acknowledged the event.

Then verify downstream completion in the mock ERP database:

```bash
docker compose --project-name northstar-release-verify exec -T postgres \
  psql -U "$PG_USERNAME" -d "$PG_DATABASE" \
  -c "SELECT id, source_customer_id, business_id, name, billing_city
      FROM erp_customers
      WHERE source_customer_id = '$SALESFORCE_TEST_ACCOUNT_ID';"

docker compose --project-name northstar-release-verify exec -T postgres \
  psql -U "$PG_USERNAME" -d "$PG_DATABASE" \
  -c "SELECT event_id, source_customer_id, processed_at
      FROM processed_customer_sync_events
      WHERE source_customer_id = '$SALESFORCE_TEST_ACCOUNT_ID';"
```

Expected: one customer and one receipt for the returned event ID.

## 5. Repeated synchronization

Run the same POST again. It creates a new event because it is a new sync
request, but must not create a second ERP customer.

Re-run the two database queries. Expected:

- exactly one customer for the Salesforce source customer ID;
- the existing customer's mutable fields contain the latest valid values;
- two processing receipts with distinct event IDs.

This proves business idempotency. Exact Kafka redelivery is a different case:
the automated integration test proves that replaying the same `eventId` keeps
both customer and receipt counts unchanged.

## 6. Validation failure and recovery

The default automated suite is the release evidence for this path. It proves
that a Salesforce Account without `businessId` returns `422`, creates no event
or integration audit, and publishes nothing. At the consumer boundary it also
proves that an invalid event is attempted once, persists no customer or
receipt, and reaches the DLT with safe category `VALIDATION`.

Recovery is deliberate: correct `Business_ID__c` on the designated Salesforce
test Account and invoke the HTTP trigger again. The correction produces a new,
valid event; the unchanged invalid DLT event must not be replayed. The happy-path
checks above prove the valid post-correction path. Do not modify Salesforce as
part of routine release verification unless that exact test-data change has
been authorized.

For recoverable infrastructure failures, follow `docs/OPERATIONS.md`. The
automated broker-level test proves one temporary failure succeeds on its second
attempt with one customer, one receipt, and no DLT record.

## 7. Cleanup

Stop both application processes, then remove only the isolated release
infrastructure and its disposable data volume:

```bash
docker compose --project-name northstar-release-verify down --volumes
```

Never substitute the normal Compose project name in this cleanup command unless
deleting its development data is intentional.

## Recorded release evidence — 2026-09-05

- [x] `./mvnw verify` passed for both modules, including formatting, automated
  tests, and executable JAR packaging.
- [x] An isolated Compose project started Kafka and a new PostgreSQL volume.
- [x] Both services applied Flyway migrations from empty databases and reported
  readiness `UP`.
- [x] The designated Salesforce Account completed the live trigger-to-ERP flow;
  its integration audit reported `PUBLISHED` and the ERP stored one customer
  plus one event receipt.
- [x] A second trigger retained one ERP customer and produced a second distinct
  successful event receipt.
- [x] Automated tests exercised missing-business-ID rejection, DLT behavior,
  corrected valid input, bounded retries, recovery, and exact-event deduplication.
- [x] `.env` was untracked and absent from Git history; actual configured OAuth
  values were absent from every reachable commit; tracked files contained no
  detected private Salesforce host, local home path, or token-like header.
- [x] Live HTTP responses and application logs inspected during verification
  did not expose OAuth credentials or Salesforce instance details.

One local setup defect was detected and corrected for the verification run:
the integration-specific database username and password still contained the
example placeholders. A fresh clone must replace all placeholder values before
startup; the application correctly failed closed instead of silently using an
unexpected credential.
