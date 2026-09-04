# Northstar Operations Runbook

This runbook covers deliberate diagnosis and manual recovery of mock ERP
customer-sync records that have reached Kafka's dead-letter topic (DLT).

The default topics are:

- source: `northstar.customer-sync.v1`
- dead letter: `northstar.customer-sync.v1.DLT`

Use the configured `CUSTOMER_SYNC_TOPIC` and
`CUSTOMER_SYNC_DEAD_LETTER_SUFFIX` values if the defaults have been changed.

## Safety rules

- Run these commands only against the intended local or test environment.
- Do not paste OAuth tokens, client secrets, authorization headers, or database
  passwords into Kafka records, commands, tickets, or logs.
- Treat customer payloads as business data. Inspect them only in an authorized
  terminal and do not commit copied payloads to the repository.
- Never connect the DLT directly back to the source topic. Every replay must be
  a deliberate response to a diagnosed and corrected failure.
- Replay one record first, verify the result, and only then consider another.

## 1. Start and identify the environment

Start Kafka and PostgreSQL if they are not already running:

```bash
docker compose up -d broker postgres
docker compose ps
```

Load the local ignored environment file into the current shell when database
verification needs its values:

```bash
set -a
source .env
set +a
```

On a new PostgreSQL volume, the Compose initialization script creates the
separate `integration_service` database automatically. If the volume predates
that script, create the database once with the existing local PostgreSQL role:

```bash
docker compose exec -T postgres \
  createdb -U "$PG_USERNAME" --owner="$PG_USERNAME" integration_service
```

Skip this command if the database already exists. The integration service and
mock ERP share one local PostgreSQL server for convenience but use separate
databases and separate Flyway histories. Neither application queries the
other's database.

Confirm the effective topic names before consuming or producing records:

```bash
SOURCE_TOPIC="${CUSTOMER_SYNC_TOPIC:-northstar.customer-sync.v1}"
DLT_SUFFIX="${CUSTOMER_SYNC_DEAD_LETTER_SUFFIX:-.DLT}"
DLT_TOPIC="${SOURCE_TOPIC}${DLT_SUFFIX}"
printf 'source=%s\ndlt=%s\n' "$SOURCE_TOPIC" "$DLT_TOPIC"
```

## 2. Inspect a dead-letter record

Read DLT records without joining the mock ERP consumer group:

```bash
docker compose exec -T broker \
  /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic "$DLT_TOPIC" \
  --from-beginning \
  --max-messages 1 \
  --property print.timestamp=true \
  --property print.partition=true \
  --property print.offset=true \
  --property print.headers=true \
  --property print.key=true \
  --property print.value=true
```

Record these values in an approved operational workspace, not in source
control:

- DLT partition and offset, which identify the failed Kafka record;
- `northstar-failure-category`, which is either `VALIDATION` or
  `RETRY_EXHAUSTED`;
- original message key (`businessId`);
- `eventId`, `correlationId`, and `customer.sourceCustomerId` from the event;
- original topic, partition, and offset headers added by Spring Kafka.

The DLT deliberately omits exception messages and stack traces. Diagnose the
cause using the safe failure category, correlation/event identifiers, service
health, and sanitized application logs. Do not respond by enabling sensitive
exception headers.

If the first record is already understood, change the consumer command to read
the required record deliberately. Do not bulk-replay every record merely
because it exists in the DLT.

## 3. Correct the underlying failure

For `RETRY_EXHAUSTED`, restore the failed dependency before replaying. Examples
include making PostgreSQL available, correcting connectivity, or resolving a
temporary infrastructure fault. Verify the mock ERP can reach Kafka and its
database before proceeding.

For `VALIDATION`, do not replay the unchanged event: the same invalid payload
will fail validation again. Correct the designated Salesforce test Account and
invoke the integration-service HTTP trigger to create a new valid event. Keep
the original DLT record as evidence of the rejected event.

Do not edit identity or versioning fields merely to force acceptance. A
manually modified event is a new event and must have a new `eventId`, a valid
contract, and a documented reason.

## 4. Replay one recoverable event

This step is intended for a preserved, valid event whose external failure has
been corrected. Copy only its original key and JSON value from the inspected
DLT record:

```bash
REPLAY_KEY='replace-with-original-business-id-key'
REPLAY_EVENT='replace-with-original-single-line-json-event'
```

Check the values before publishing. The event must retain its original
`eventId`, `correlationId`, and customer payload:

```bash
printf 'key=%s\nevent=%s\n' "$REPLAY_KEY" "$REPLAY_EVENT"
```

Republish the key and event to the source topic:

```bash
printf '%s\t%s\n' "$REPLAY_KEY" "$REPLAY_EVENT" | \
  docker compose exec -T broker \
    /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server localhost:9092 \
    --topic "$SOURCE_TOPIC" \
    --property parse.key=true \
    --property key.separator=$'\t'
```

Publishing to the source topic is intentional: it passes through the normal
listener, validation, transaction, idempotency, retry, and DLT behavior. Never
publish directly to PostgreSQL or the processing-receipt table.

## 5. Verify recovery

Set the identifiers from the replayed event:

```bash
REPLAY_EVENT_ID='replace-with-event-uuid'
REPLAY_SOURCE_CUSTOMER_ID='replace-with-salesforce-account-id'
```

Verify that one customer exists with the expected current values:

```bash
docker compose exec -T postgres \
  psql -U "$PG_USERNAME" -d "$PG_DATABASE" \
  -c "SELECT id, source_customer_id, business_id, name, billing_city
      FROM erp_customers
      WHERE source_customer_id = '$REPLAY_SOURCE_CUSTOMER_ID';"
```

Verify that successful processing recorded the replayed event ID:

```bash
docker compose exec -T postgres \
  psql -U "$PG_USERNAME" -d "$PG_DATABASE" \
  -c "SELECT event_id, source_customer_id, processed_at
      FROM processed_customer_sync_events
      WHERE event_id = '$REPLAY_EVENT_ID'::uuid;"
```

Verify the source-customer idempotency invariant:

```bash
docker compose exec -T postgres \
  psql -U "$PG_USERNAME" -d "$PG_DATABASE" \
  -c "SELECT source_customer_id, COUNT(*)
      FROM erp_customers
      WHERE source_customer_id = '$REPLAY_SOURCE_CUSTOMER_ID'
      GROUP BY source_customer_id;"
```

The expected result is one customer row and one receipt for the replayed
`eventId`. Replaying that exact successful event again is safe: the receipt
causes the handler to skip repeated customer work.

Kafka topics are append-only logs, so a successful replay does not remove the
original DLT record. Recovery is demonstrated by the successful receipt and
customer state, not by an empty DLT.

## 6. If replay fails again

Stop replaying. Inspect the newly appended DLT record and compare its category,
event ID, and correlation ID with the original. Repeated DLT records mean the
cause was not corrected or a different failure was introduced. Do not create
an automated DLT-to-source loop; it would bypass deliberate diagnosis and can
retry poison data forever.
