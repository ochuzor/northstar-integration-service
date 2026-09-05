# Northstar Integration Service — Learning Roadmap

## How to use this roadmap

Complete one milestone at a time. Each milestone ends with observable behavior,
focused tests, a review, and an update to the project brief. Do not install all
eventual dependencies during project creation.

The roadmap is ordered to keep unfamiliar concepts separate: external HTTP and
OAuth first, transformation second, messaging third, persistence fourth, and
distributed reliability only after the happy path exists.

## Milestone 0 — Define and scaffold the first slice

### Learn and decide

- Chosen trigger: `POST /api/sync/account/{salesforceAccountId}`.
- Chosen structure: one repository with separate Maven modules, introduced only
  when their corresponding slice begins.
- Chosen read-only response fields: Salesforce Account ID, name, business ID,
  and billing city. The Salesforce business-ID field API name is
  `Business_ID__c`.
- Selected Spring Boot 4.1.0 through Spring Initializr.

### Build

- Generated the Spring Boot integration service with Java 21, Maven, Spring Web
  MVC, and Validation.
- Added durable ignore rules and a placeholder-only environment-variable
  example.
- Confirmed context startup, Maven verification, and executable packaging from
  the repository root.

**Status:** Complete.

### Exit criteria

- The architectural choices and reasons are recorded.
- A fresh clone builds without Salesforce credentials.
- No Salesforce, Kafka, or database behavior has been implied but not built.

## Milestone 1 — Authenticate with Salesforce

### Learn

- OAuth 2.0 Client Credentials request and token response.
- Form-encoded HTTP requests.
- Typed Spring configuration and secret injection.
- Safe token and authorization-header logging rules.

### Build

- Bound and validated Salesforce OAuth configuration using environment-backed
  typed properties.
- Defined the external token-response DTO and mapped its safe metadata,
  including issued-at epoch milliseconds, to an authentication result.
- Request and deserialize an access token through a dedicated `RestClient`
  boundary using a form-encoded client-credentials request.
- Expose the result only as safe metadata and suppress HTTP-client DEBUG logging
  that could otherwise reveal the form-encoded client secret.
- Translate client-error OAuth responses into a sanitized authentication
  exception with the HTTP status code.
- Reject malformed or incomplete successful token responses as sanitized
  authentication failures.
- Translate server-error responses into sanitized authentication failures with
  their real HTTP status.
- Translate connection failures into a separate safe unavailable-server
  exception without inventing an HTTP status.

### Test

- Added focused configuration tests for successful binding, missing or blank
  required values, and malformed token-URL conversion.
- Added a stubbed-HTTP success test that verifies the token URL, POST method,
  form content type, client-credentials fields, deserialization, and safe
  result mapping.
- Added a stubbed-HTTP test for a rejected client-credentials request, ensuring
  its sanitized exception retains status `400` without leaking remote error
  details or credentials.
- Added stubbed-HTTP tests for malformed JSON and incomplete successful token
  responses, both producing sanitized exceptions with status `200`.
- Added a stubbed `503` server-error test and a stubbed connection-failure test
  for the unavailable authentication server case.
- One deliberate live Salesforce smoke test outside the default automated
  suite.

### Exit criteria

- Authentication works from Java against the test Salesforce organization.
- Default tests are deterministic and do not require Salesforce.
- Secrets and tokens do not appear in source control or logs.

**Status:** Complete. The default suite is deterministic and the separately
tagged live smoke test authenticated successfully against the Salesforce test
organization.

## Milestone 2 — Fetch one Salesforce Account

### Learn

- Bearer-token requests and Salesforce instance URLs.
- SOQL query encoding.
- External API DTOs and Salesforce query-result envelopes.
- Remote error translation and API-version configuration.

### Build

- Fetch the designated test Account through a dedicated Salesforce client.
  **Complete at the stubbed HTTP boundary.**
- Map the Salesforce JSON response into typed external DTOs. **Complete.**
- Return or log a deliberately safe read-only result through the chosen trigger.
  **Complete through the HTTP controller.**
- Define not-found, multiple-result, unauthorized, throttled, malformed, and
  unavailable response behavior. **Complete at the client boundary.**

### Test

- Stub-server tests for request construction, mapping, and error cases.
  **Complete for the Account client.**
- Focused application-service and MVC tests for orchestration, safe success
  output, and sanitized HTTP error translation. **Complete.**
- One deliberate live happy-path smoke test.
  **Complete against the designated Salesforce test Account.**

### Exit criteria

- The first vertical slice reliably authenticates, fetches, maps, and exposes
  the test Account without Kafka or PostgreSQL.

**Status:** Complete. The deterministic suite verifies the HTTP, orchestration,
and external-client boundaries, and the opt-in live smoke test has retrieved
the designated Account through the real Java application flow.

## Milestone 3 — Transform and validate an internal customer

### Learn

- Anti-corruption boundaries between Salesforce and internal models.
- Mapping versus validation responsibilities.
- Field normalization and explicit missing-data policy.

Agreed policy: the internal customer requires source customer ID, business ID,
and name; billing city is optional. Mapping trims surrounding whitespace,
converts blank billing city to `null`, and preserves casing. Jakarta Bean
Validation runs after mapping and reports invalid customers through a dedicated
safe exception.

### Build

- Define the minimum internal customer model required by the mock ERP.
  **Complete.**
- Transform Salesforce Account data into that model. **Complete.**
- Validate required fields, including the planned missing `businessId` failure.
  **Complete at the application boundary.**
- Produce an explicit success or validation-failure result. **Complete through
  the synchronization HTTP boundary.**

### Test

- Focused mapper boundary tests. **Complete.**
- Validation tests for complete, missing, malformed, and normalized fields.
  **Complete for the agreed required and optional fields.**

### Exit criteria

- Salesforce-specific DTOs do not leak into the internal event contract.
- Invalid customers are rejected observably and are not silently discarded.

**Status:** Complete. Salesforce Account data is mapped into a validated,
Salesforce-independent customer through the application boundary. The trigger
returns the accepted safe customer response on success and a sanitized `422`
response for invalid customer data. Focused mapper, validator, orchestration,
and MVC tests cover the behavior.

## Milestone 4 — Publish a versioned Kafka event

Agreed contract: publish `CUSTOMER_SYNC_REQUESTED` version `1` events to the
configurable `northstar.customer-sync.v1` topic using `businessId` as the Kafka
message key. Each self-describing envelope carries distinct event and
correlation UUIDs, an `Instant` timestamp, source `SALESFORCE`, and a customer
payload separate from the internal domain model. Time and UUID generation must
be controllable in tests. The HTTP trigger will report broker-acknowledged
publication as `202 Accepted`, not completed ERP synchronization.

### Learn

- Topics, partitions, keys, offsets, consumer groups, and delivery semantics.
- Event contracts versus internal domain objects.
- Message keys and ordering.

### Build

- Add Kafka to local Docker infrastructure. **Complete with a single-node
  KRaft broker in Docker Compose.**
- Add Spring Kafka, typed topic configuration, and deterministic production
  event-factory wiring. **Complete without requiring a broker at startup.**
- Define a small versioned customer-synchronization event envelope.
  **Complete.**
- Publish valid transformed customers using a deliberate message key.
  **Complete at the mocked producer boundary.**
- Define behavior when publication fails. **Complete at the producer boundary
  for rejection, timeout, immediate failure, and interruption.**
- Orchestrate customer preparation, event creation, and acknowledged
  publication. **Complete at the application-service boundary.**
- Connect the HTTP trigger to the complete synchronization flow and return a
  truthful `202 Accepted` publication receipt or sanitized `503` publication
  failure. **Complete at the MVC boundary.**

### Test

- Serialization contract tests. **Complete with Jackson 3.**
- Producer-focused tests. **Complete.**
- MVC tests for the acknowledged publication receipt and publication-failure
  response. **Complete.**
- A narrow Kafka integration test against disposable or local infrastructure.
  **Complete with an embedded KRaft broker.**

### Exit criteria

- A valid Salesforce customer produces a documented event on Kafka.
- Event version, identity, correlation, and timestamp semantics are explicit.

**Status:** Complete. Local Kafka infrastructure is reproducible, and the
automated integration test verifies acknowledged publication, the message key,
and the deserialized event contract against a real embedded broker.

## Milestone 5 — Build the mock ERP consumer and persistence slice

### Learn

- Consumer-group behavior and offset acknowledgement.
- Database transaction boundaries around message processing.
- Flyway-managed schemas and JPA persistence in a consumer application.

### Build

- Scaffold the mock ERP service at the agreed boundary. **Complete as a
  separately runnable Java 21 Spring Boot Maven module.**
- Add a configurable Kafka listener that deserializes and delegates the
  customer event. **Complete at the listener unit boundary and disabled by
  default until durable handling exists.**
- Validate and handle the consumed customer event. **Complete through a
  normalized ERP domain model and application-handler boundary.**
- Store the ERP customer in PostgreSQL through a Flyway-managed schema.
  **Complete through the transactional application-handler boundary.**
- Keep event DTOs separate from persistence entities. **Complete across the
  messaging, domain, and JPA boundaries.**

### Test

- Consumer-owned event contract deserialization test. **Complete with Jackson
  3 and the version-one producer JSON shape.**
- Typed consumer-configuration and listener-delegation tests. **Complete.**
- Consumer mapping tests. **Complete for mapping, normalization, validation,
  handler ordering, persistence, validation short-circuiting, and database
  failure propagation.**
- PostgreSQL repository tests. **Complete with Flyway and Testcontainers.**
- Kafka-to-database integration test for the happy path. **Complete with an
  embedded Kafka broker and Testcontainers PostgreSQL.**

### Exit criteria

- One triggered Salesforce Account reaches PostgreSQL through Kafka.
- The stored customer can be inspected through a narrow operational read or
  database verification mechanism.

**Status:** Complete. The automated Kafka-to-PostgreSQL test passes, and a
deliberate local smoke test persisted the designated real Salesforce Account
through the HTTP trigger and Docker Compose Kafka broker. Repeating the trigger
created duplicate rows as expected before Milestone 6.

## Milestone 6 — Make consumption idempotent

### Learn

- At-least-once delivery and why duplicate events are normal.
- Natural, source, event, and idempotency identifiers.
- Database uniqueness and atomic processing records.

### Build

- Choose and document the idempotency key. **Complete for the single-source MVP:
  use Salesforce `sourceCustomerId` with update semantics and database
  uniqueness.**
- Make duplicate delivery safe under the chosen update semantics. **Complete at
  the transactional handler and database-constraint boundaries; exact repeated
  Kafka delivery is proven by an integration test.**
- Record successful processing state and use it to identify exact duplicate
  events. **Complete with a transactional receipt keyed by `eventId`; failed
  transactions intentionally leave no success receipt. Durable failure records
  move to Milestone 7 with retry and dead-letter handling.**

### Test

- Deliver the same event repeatedly. **Complete for both exact event redelivery
  and repeated source-customer delivery through Kafka with changed mutable
  values.**
- Verify no duplicate ERP customer and no incorrect repeated side effect.
  **Complete: the integration test retains one row and its original database
  identity while applying the latest values.**
- Exercise a concurrency-relevant duplicate scenario where practical.
  **Complete at the database boundary: the unique constraint rejects a second
  row with the same source customer ID.**

### Exit criteria

- Duplicate Kafka delivery has a deterministic documented outcome.

**Status:** Complete. Customer persistence has deterministic create-or-update
semantics, successful event IDs are recorded atomically with customer changes,
and exact known event IDs are skipped without repeating customer work.

## Milestone 7 — Add bounded retries and dead-letter handling

### Learn

- Transient versus permanent failures.
- Retry backoff, attempt limits, poison messages, and dead-letter topics.
- Why validation failures and infrastructure failures need different policies.

### Build

- Classify retryable and non-retryable failures. **Decision complete:
  validation is non-retryable; infrastructure and unexpected runtime failures
  are retryable with three total attempts and one-second fixed backoff.**
- Add bounded retry behavior and a dead-letter topic. **Complete for exhausted
  retryable and permanent validation failures at the broker boundary.**
- Preserve original event, correlation, failure category, and useful error
  metadata without leaking secrets. **Configured to preserve the original
  record, omit exception messages and stack traces, and add a safe failure
  category; broker-level tests verify the preserved record, safe category, and
  omitted exception message and stack trace.**
- Define a small manual replay or recovery procedure. **Complete in
  `docs/OPERATIONS.md` with safe inspection, failure-specific correction,
  deliberate single-record replay, idempotency verification, and loop
  prevention.**

### Test

- Temporary failure followed by success. **Complete with one simulated failure,
  success on the second handler attempt, exactly one customer and processing
  receipt, and no DLT record.**
- Exhausted transient failure routed to dead letter. **Complete with three
  observed handler attempts and no persisted customer or processing receipt.**
- Permanent validation failure routed without pointless repeated attempts.
  **Complete with one observed handler attempt and no persisted customer or
  processing receipt.**

### Exit criteria

- No tested failure disappears silently or retries forever.
- Dead-lettered work can be diagnosed and deliberately recovered.

**Status:** Complete. Broker-level tests prove successful retry, exhausted
retry, and non-retryable validation behavior. The operations runbook documents
safe diagnosis and deliberate recovery without automatic replay loops.

## Milestone 8 — Add synchronization auditability and observability

### Learn

- Correlation IDs, structured logs, health indicators, and audit histories.
- Difference between operational telemetry and business audit state.

### Build

- Define initiated, published, consumed, succeeded, and failed status semantics.
  **Decision complete: the integration service owns durable `INITIATED`,
  `PUBLISHED`, and `PUBLICATION_FAILED`; the existing mock ERP receipt proves
  `SUCCEEDED`; the DLT proves terminal `FAILED`; and transient `CONSUMED` state
  is not persisted. Evidence is connected by event, correlation, and source
  customer identifiers.**
- Persist or expose the minimum useful synchronization audit history.
  **Persistence foundation complete for integration-owned evidence: a separate
  integration-service database, Flyway-managed audit table, JPA entity, and
  correlation/event repository lookups are verified against PostgreSQL.
  Explicit transactions now record and transition `INITIATED`, `PUBLISHED`,
  and `PUBLICATION_FAILED` through the synchronization workflow; the safe
  lookup endpoint is verified through PostgreSQL and HTTP with an exact safe
  response-field contract.**
- Add structured, correlated logs and relevant health information.
  **Structured lifecycle logging complete: both services emit distinct safe
  lifecycle events with event, correlation, and source-customer identifiers.
  Safe liveness and database-backed readiness probes are exposed through the
  health-only Actuator boundary, and bounded Kafka connectivity now contributes
  to readiness without affecting liveness or revealing dependency details.**
- Redact secrets and sensitive remote response content. **Lifecycle-log tests
  verify that business IDs, customer names, billing cities, Kafka keys, and
  exception details are omitted; final repository and runtime-log review
  remains.**

### Test

- Verify status transitions for happy, invalid, duplicate, retried, and
  dead-lettered flows. **Complete. The observability matrix maps every outcome
  to its durable or operational evidence and the focused automated tests that
  prove it.**

### Exit criteria

- An operator can answer what happened to a synchronization request without
  reading raw database tables or guessing from unrelated logs.

**Status:** Complete. Integration audit state, ERP processing receipts, safe
DLT records, correlated lifecycle logs, and dependency-aware readiness provide
explicit evidence for every supported outcome. Exact event redelivery is also
verified through Kafka without repeating persistence.

## Milestone 9 — Final end-to-end verification and portfolio release

### Build and verify

- Run the complete flow from the chosen trigger to PostgreSQL. **Complete with
  the designated Salesforce test Account and isolated local infrastructure.**
- Repeat it to demonstrate idempotency. **Complete: two distinct events left
  one current ERP customer and two successful processing receipts.**
- Exercise the missing-business-ID failure and recovery path. **Complete
  through deterministic boundary and broker-level tests; routine verification
  does not mutate Salesforce merely to provoke invalid test data.**
- Verify fresh infrastructure startup and Flyway migrations. **Complete with a
  separate Compose project and new PostgreSQL volume.**
- Run full Maven verification, formatting, and executable packaging.
  **Complete through the root `./mvnw verify` release command.**
- Audit tracked files and Git history for secrets and environment-specific data.
  **Complete without exposing the compared local secret values.**

### Document

- Architecture diagram and component responsibilities. **Complete in the root
  README.**
- Local prerequisites, environment variables, and startup order. **Complete in
  the README and release checklist.**
- Salesforce test-organization setup without credentials. **Complete.**
- Kafka topics, event contract, retry/DLT policy, and recovery steps.
  **Complete across the README and operations runbook.**
- API or trigger examples, tests, limitations, and future work. **Complete.**
- Honest AI-assisted development approach and portfolio claims. **Complete.**

### Exit criteria

- A new developer can clone, configure, run, test, and understand the project.
- The repository demonstrates the complete narrow integration flow.
- CV and LinkedIn statements claim only behavior proven by code and tests.

**Status:** Complete. The release checklist records a passing clean-infrastructure
verification on 2026-09-05, and the root README provides the architecture,
setup, operating model, verified capabilities, limitations, and development
disclosure needed for an honest portfolio presentation.
