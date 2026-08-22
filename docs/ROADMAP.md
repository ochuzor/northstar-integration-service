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
- Define not-found, multiple-result, unauthorized, throttled, malformed, and
  unavailable response behavior. **Complete at the client boundary.**

### Test

- Stub-server tests for request construction, mapping, and error cases.
  **Complete for the Account client.**
- One deliberate live happy-path smoke test.

### Exit criteria

- The first vertical slice reliably authenticates, fetches, maps, and exposes
  the test Account without Kafka or PostgreSQL.

## Milestone 3 — Transform and validate an internal customer

### Learn

- Anti-corruption boundaries between Salesforce and internal models.
- Mapping versus validation responsibilities.
- Field normalization and explicit missing-data policy.

### Build

- Define the minimum internal customer model required by the mock ERP.
- Transform Salesforce Account data into that model.
- Validate required fields, including the planned missing `businessId` failure.
- Produce an explicit success or validation-failure result.

### Test

- Focused mapper boundary tests.
- Validation tests for complete, missing, malformed, and normalized fields.

### Exit criteria

- Salesforce-specific DTOs do not leak into the internal event contract.
- Invalid customers are rejected observably and are not silently discarded.

## Milestone 4 — Publish a versioned Kafka event

### Learn

- Topics, partitions, keys, offsets, consumer groups, and delivery semantics.
- Event contracts versus internal domain objects.
- Message keys and ordering.

### Build

- Add Kafka to local Docker infrastructure.
- Define a small versioned customer-synchronization event envelope.
- Publish valid transformed customers using a deliberate message key.
- Define behavior when publication fails.

### Test

- Serialization contract tests.
- Producer-focused tests.
- A narrow Kafka integration test against disposable or local infrastructure.

### Exit criteria

- A valid Salesforce customer produces a documented event on Kafka.
- Event version, identity, correlation, and timestamp semantics are explicit.

## Milestone 5 — Build the mock ERP consumer and persistence slice

### Learn

- Consumer-group behavior and offset acknowledgement.
- Database transaction boundaries around message processing.
- Flyway-managed schemas and JPA persistence in a consumer application.

### Build

- Scaffold the mock ERP service at the agreed boundary.
- Consume and validate the customer event.
- Store the ERP customer in PostgreSQL through a Flyway-managed schema.
- Keep event DTOs separate from persistence entities.

### Test

- Consumer mapping tests.
- PostgreSQL repository tests.
- Kafka-to-database integration test for the happy path.

### Exit criteria

- One triggered Salesforce Account reaches PostgreSQL through Kafka.
- The stored customer can be inspected through a narrow operational read or
  database verification mechanism.

## Milestone 6 — Make consumption idempotent

### Learn

- At-least-once delivery and why duplicate events are normal.
- Natural, source, event, and idempotency identifiers.
- Database uniqueness and atomic processing records.

### Build

- Choose and document the idempotency key.
- Make duplicate delivery safe under the chosen update semantics.
- Record enough processing state to distinguish duplicate, succeeded, and
  failed attempts.

### Test

- Deliver the same event repeatedly.
- Verify no duplicate ERP customer and no incorrect repeated side effect.
- Exercise a concurrency-relevant duplicate scenario where practical.

### Exit criteria

- Duplicate Kafka delivery has a deterministic documented outcome.

## Milestone 7 — Add bounded retries and dead-letter handling

### Learn

- Transient versus permanent failures.
- Retry backoff, attempt limits, poison messages, and dead-letter topics.
- Why validation failures and infrastructure failures need different policies.

### Build

- Classify retryable and non-retryable failures.
- Add bounded retry behavior and a dead-letter topic.
- Preserve original event, correlation, failure category, and useful error
  metadata without leaking secrets.
- Define a small manual replay or recovery procedure.

### Test

- Temporary failure followed by success.
- Exhausted transient failure routed to dead letter.
- Permanent validation failure routed without pointless repeated attempts.

### Exit criteria

- No tested failure disappears silently or retries forever.
- Dead-lettered work can be diagnosed and deliberately recovered.

## Milestone 8 — Add synchronization auditability and observability

### Learn

- Correlation IDs, structured logs, health indicators, and audit histories.
- Difference between operational telemetry and business audit state.

### Build

- Define initiated, published, consumed, succeeded, and failed status semantics.
- Persist or expose the minimum useful synchronization audit history.
- Add structured, correlated logs and relevant health information.
- Redact secrets and sensitive remote response content.

### Test

- Verify status transitions for happy, invalid, duplicate, retried, and
  dead-lettered flows.

### Exit criteria

- An operator can answer what happened to a synchronization request without
  reading raw database tables or guessing from unrelated logs.

## Milestone 9 — Final end-to-end verification and portfolio release

### Build and verify

- Run the complete flow from the chosen trigger to PostgreSQL.
- Repeat it to demonstrate idempotency.
- Exercise the missing-business-ID failure and recovery path.
- Verify fresh infrastructure startup and Flyway migrations.
- Run full Maven verification, formatting, and executable packaging.
- Audit tracked files and Git history for secrets and environment-specific data.

### Document

- Architecture diagram and component responsibilities.
- Local prerequisites, environment variables, and startup order.
- Salesforce test-organization setup without credentials.
- Kafka topics, event contract, retry/DLT policy, and recovery steps.
- API or trigger examples, tests, limitations, and future work.
- Honest AI-assisted development approach and portfolio claims.

### Exit criteria

- A new developer can clone, configure, run, test, and understand the project.
- The repository demonstrates the complete narrow integration flow.
- CV and LinkedIn statements claim only behavior proven by code and tests.
