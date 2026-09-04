# Northstar Integration Service — Project Brief

## Product purpose

Build a deliberately small but realistic enterprise integration that moves
customer data from Salesforce into a fictional internal ERP system.

The project is both a learning exercise and a portfolio project. Its purpose is
to develop credible hands-on experience with external REST APIs, OAuth 2.0,
data transformation, asynchronous messaging, failure handling, idempotency,
auditability, and integration testing with Java and Spring Boot.

## Business scenario

Northstar Supplies is a fictional wholesale company. Salesforce is its CRM,
while a mock ERP represents internal warehouse and finance systems.

The intended end-to-end flow is:

```text
Salesforce
   ↓
Northstar integration service
   ↓
Kafka
   ↓
Mock ERP service
   ↓
PostgreSQL
```

## MVP outcome

For one supported Salesforce Account synchronization request, the completed MVP
must:

1. authenticate with Salesforce using OAuth 2.0 Client Credentials;
2. retrieve the Account through the Salesforce REST API;
3. map the external representation to an internal customer model;
4. validate the customer required by the mock ERP;
5. publish a versioned customer synchronization event to Kafka;
6. consume the event in the mock ERP service;
7. store the customer idempotently in PostgreSQL;
8. record an observable succeeded or failed synchronization outcome;
9. preserve failed messages through deliberate retry and dead-letter behavior.

Synchronization is explicitly requested through
`POST /api/sync/account/{salesforceAccountId}`. This does not imply real-time
Salesforce change capture, which the project has not implemented.

## Current scope

- One-way Salesforce Account to mock ERP customer synchronization.
- A narrow subset of Salesforce Account fields.
- OAuth 2.0 Client Credentials.
- REST retrieval from Salesforce.
- Explicit external-to-internal mapping and validation.
- Kafka event publication and consumption.
- PostgreSQL persistence owned by the mock ERP.
- Idempotency, bounded retry, dead-letter handling, audit status, and useful
  structured logs.
- Automated tests at the appropriate boundaries.
- Docker-based local infrastructure where it improves reproducibility.

## Current exclusions

Until explicitly brought into scope:

- A frontend.
- Real SAP or another real ERP.
- Bidirectional synchronization.
- A general-purpose integration platform.
- Multiple Salesforce objects or arbitrary SOQL execution.
- Salesforce production data.
- Real-time change-data capture unless deliberately selected and implemented.
- Kubernetes, cloud deployment, schema registries, distributed tracing, and
  unrelated enterprise infrastructure.
- Features that do not contribute to the agreed MVP flow.

## Current progress

- A Salesforce Developer Edition organization exists.
- A Salesforce External Client App is configured for OAuth 2.0 Client
  Credentials.
- Token retrieval, Salesforce API discovery, an Account SOQL query, and reading
  designated test Account data have been verified manually with `curl`.
- Salesforce credentials and environment-specific URLs remain private and must
  not be committed.
- The initial synchronization trigger will be
  `POST /api/sync/account/{salesforceAccountId}`.
- The integration service and future mock ERP will live in one repository as
  separate Maven modules, while only the integration service will be
  scaffolded for the first slice.
- The first Account read will expose a safe representation containing the
  Salesforce Account ID, name, business ID, and billing city. The Salesforce
  API name of the business-ID field is `Business_ID__c`.
- A Java 21 integration-service module has been scaffolded with Spring Boot
  4.1.0, Maven, Spring Web MVC, and Validation.
- A repository-root Maven aggregator and Maven Wrapper build the service module.
- `./mvnw verify` succeeds from the repository root, including the Spring
  context-load test and executable JAR repackaging.
- Durable ignore rules and a placeholder-only `.env.example` protect and
  document the local Salesforce configuration boundary.
- Salesforce OAuth settings bind through validated typed configuration. Missing
  or blank required values and malformed token URLs fail during startup, while
  focused tests use synthetic values and require no live credentials.
- The Salesforce token-response DTO and safe authentication-result contract are
  separate, and issued-at epoch milliseconds map explicitly to `Instant`
  without copying the access token or instance URL into the safe result.
- The Salesforce OAuth client sends a client-credentials token request as
  `application/x-www-form-urlencoded` through Spring's `RestClient`. A focused
  stubbed-HTTP test verifies the request and safe response mapping without live
  Salesforce access.
- HTTP-client DEBUG logging is explicitly suppressed so the form-encoded
  client secret is not exposed even when broader application debugging is
  enabled.
- Salesforce client-error OAuth responses are translated into a sanitized
  `SalesforceAuthenticationException` containing a structured HTTP status code;
  remote response bodies and credentials are not exposed.
- Malformed JSON and incomplete successful token responses are rejected as
  sanitized authentication failures rather than leaking JSON, null, or numeric
  conversion exceptions.
- OAuth server `5xx` responses are translated into sanitized authentication
  failures with their real status code, while connection failures are reported
  through a separate safe unavailable-server exception.
- A tagged, opt-in Java smoke test has authenticated successfully against the
  Salesforce test organization. The default Maven verification excludes that
  live test and remains deterministic without Salesforce credentials.
- `SalesforceSession` now carries the validated token and instance URL only
  between trusted Salesforce client boundaries; the safe authentication result
  remains free of credentials.
- Account retrieval will use Salesforce's REST Query API with a required,
  environment-backed API version and a typed query-result envelope. The query
  will select only `Id`, `Name`, `Business_ID__c`, and `BillingCity`.
- The dedicated Salesforce Account client now constructs an encoded SOQL query
  against the authenticated instance URL, sends the bearer token, and maps a
  single Account into a typed external response.
- Focused stubbed-HTTP tests cover the Account happy path, invalid IDs, empty,
  malformed, incomplete, zero-result, and multiple-result responses, HTTP
  `401`, `429`, and `503` failures, and connection unavailability. HTTP failures
  retain a typed status without exposing remote bodies, tokens, or instance
  URLs; connection failures do not invent an HTTP status.
- Spotless enforces the committed Eclipse formatter profile during Maven
  verification, and committed VS Code settings use the same profile on save.
- Maven Surefire starts Mockito explicitly as a Java agent on Java 21, avoiding
  deprecated dynamic self-attachment and keeping Mockito-based tests reliable
  in restricted build environments.
- An application service now orchestrates OAuth authentication and Account
  retrieval while mapping the Salesforce DTO to the four-field safe result.
- `POST /api/sync/account/{salesforceAccountId}` exposes that safe result. A
  global HTTP exception boundary returns stable sanitized `400`, `404`, `502`,
  and `503` error contracts without exposing upstream bodies, credentials, or
  private instance details.
- A tagged, opt-in Account smoke test has successfully exercised the real Java
  application flow from OAuth authentication through Salesforce Account
  retrieval. The default Maven suite excludes this live test and remains
  deterministic.
- A Salesforce-independent customer model now separates the internal business
  representation from Salesforce DTOs. Mapping normalizes the accepted fields,
  and Jakarta Bean Validation rejects missing required customer data through a
  safe exception containing only invalid field names.
- A customer-preparation application service composes Account retrieval,
  mapping, and validation. The synchronization trigger now uses that service,
  preserves the accepted safe success response, and returns a sanitized `422`
  response when Salesforce data cannot produce a valid internal customer.
- A versioned, Salesforce-independent `CUSTOMER_SYNC_REQUESTED` event contract
  and customer payload are defined for Kafka. A deterministic factory uses an
  injected `Clock` and UUID generator for distinct event and correlation
  identities, and a Jackson 3 contract test verifies the complete JSON shape.
- Spring Kafka is now available without requiring a broker during application
  startup. Environment-backed bootstrap-server and validated customer-sync
  topic configuration are defined, and explicit production beans supply the
  UTC clock, random UUID generator, and event factory.
- The Kafka producer boundary publishes customer-sync events to the configured
  topic with `businessId` as the message key and waits a bounded five seconds
  for broker acknowledgement. It returns safe publication identifiers on
  success and translates rejection, timeout, immediate send failure, or thread
  interruption into a sanitized exception while preserving interrupt status.
- A customer-synchronization application service now orchestrates validated
  customer preparation, versioned event creation, and acknowledged Kafka
  publication. Preparation failures short-circuit the flow before an event is
  created or a publication is attempted.
- The HTTP synchronization trigger now invokes the complete orchestration and
  returns `202 Accepted` only after Kafka acknowledges publication. Its safe
  receipt contains the Salesforce Account ID, event ID, correlation ID, and an
  `ACCEPTED` status; publication failures return a sanitized `503` response.
- A single-node Kafka broker is available through the repository's Docker
  Compose configuration. An embedded-Kafka integration test proves that the
  producer sends the expected `businessId` key and complete Jackson 3 event
  contract to the configured customer-sync topic.
- A separately runnable Java 21 mock ERP Spring Boot module now participates in
  the root Maven build. It owns a matching customer-sync input contract, and a
  Jackson 3 test proves that version-one producer JSON can be deserialized
  without sharing Java DTO classes between services.
- The mock ERP now has validated typed topic, group, and listener-enabled
  configuration plus a Kafka listener boundary that delegates the message key
  and deserialized event to an application handler. The listener is disabled by
  default so this pre-persistence slice cannot acknowledge and discard real
  synchronization work; focused tests verify configuration and delegation.
- The mock ERP application handler now maps the messaging payload into a
  separate normalized `ErpCustomer` domain model and validates its required
  source ID, business ID, and name. Validation failures retain only safe invalid
  field names and propagate out of the handler so Kafka cannot interpret them
  as successful processing.
- PostgreSQL is now available through Docker Compose, and the mock ERP owns an
  environment-backed datasource, a Flyway-managed `erp_customers` table, and a
  JPA entity kept separate from its domain model. Testcontainers-backed
  repository tests prove required-field persistence, generated identity,
  lookup by Salesforce source customer ID, and nullable billing city behavior
  against PostgreSQL.
- The mock ERP application handler now maps validated domain customers into
  persistence entities and saves them within a Spring-managed transaction.
  Validation failures stop before persistence, and database failures propagate
  back through the listener boundary instead of being treated as successful
  message handling.
- A Kafka-to-PostgreSQL integration test now enables the listener only inside
  its test context, publishes the version-one contract as JSON to an embedded
  Kafka broker, and verifies the resulting customer row in a Testcontainers
  PostgreSQL database without mocking the consumer, handler, or repository.
- The live local flow has now been verified from the HTTP trigger and real
  Salesforce test Account through Docker Compose Kafka into PostgreSQL. The
  producer omits Java type headers and the consumer deliberately uses its own
  configured event type, preserving the service contract boundary. Repeating
  the trigger produced duplicate rows, confirming the idempotency behavior
  intentionally deferred to Milestone 6.
- Mock ERP persistence now enforces one row per Salesforce `sourceCustomerId`.
  Transactional event handling creates a missing customer or updates the
  existing row's mutable details, with focused handler and PostgreSQL constraint
  tests covering both paths.
- The Kafka-to-PostgreSQL integration test now delivers two events for the same
  source customer and proves that the second delivery retains the row's database
  identity, updates its mutable values, and leaves exactly one customer row.
- The mock ERP now uses a minimal transactional inbox record keyed by `eventId`.
  A successful customer write and its `processed_customer_sync_events` receipt
  commit together; a known event ID is skipped, while a failure rolls back both
  writes and remains eligible for Kafka redelivery. Processing time uses an
  injectable UTC `Clock` for deterministic tests.
- The mock ERP now has validated, environment-backed retry and dead-letter
  settings plus a Spring Kafka error handler. It performs two retries after the
  initial attempt, sends validation failures directly to recovery, routes other
  exhausted records to the source topic plus `.DLT`, and omits exception
  messages and stack traces while adding a safe failure-category header.
- Embedded-Kafka and Testcontainers tests prove that permanent validation
  failures reach the DLT after one attempt and exhausted retryable failures
  reach it after three attempts. Both paths preserve the original event and
  key, expose only the safe failure category, and create neither a customer nor
  a successful-processing receipt.
- A broker-level recovery test proves that a temporary failure succeeds on the
  second attempt, persists exactly one customer and processing receipt, and
  produces no dead-letter record.
- The operations runbook documents safe DLT inspection, failure-specific
  correction, deliberate one-record replay through the source topic, database
  and idempotency verification, and prevention of automatic replay loops.
- Milestone 8 audit semantics are defined at service boundaries. The integration
  service owns durable `INITIATED`, `PUBLISHED`, and `PUBLICATION_FAILED`
  records; the mock ERP's existing processing receipt is durable `SUCCEEDED`
  evidence; and the Kafka DLT is durable terminal `FAILED` evidence. A transient
  `CONSUMED` state will not be persisted because a crash could leave it stale.
  `eventId`, `correlationId`, and `sourceCustomerId` connect the evidence, and
  the integration service will expose its records through a safe status lookup.
- Integration-owned audit persistence now has an isolated PostgreSQL database,
  its own Flyway history, and a `customer_sync_audits` table. The Java boundary
  consists of an explicit status enum, a JPA entity, and lookup by correlation
  or event ID, verified against PostgreSQL with Testcontainers.
- Customer synchronization now records `INITIATED` before Kafka publication,
  `PUBLISHED` only after broker acknowledgement, and `PUBLICATION_FAILED`
  before rethrowing the existing sanitized publication exception. Audit writes
  use separate `REQUIRES_NEW` transactions and the shared injectable UTC
  `Clock`; focused and PostgreSQL-backed tests verify ordering, timestamps,
  durable final states, and the safe `KAFKA_PUBLICATION` failure category.
- `GET /api/sync/{correlationId}` now exposes integration-owned audit evidence
  through separate application and HTTP DTOs. It returns only approved IDs,
  status, timestamps, and the safe failure category; unknown correlations
  return a sanitized `404`. Focused MVC tests and a PostgreSQL-backed HTTP test
  verify the repository-to-response path and exact response-field boundary.
- Both services now emit structured customer-synchronization lifecycle logs.
  Integration-side initiated, published, and publication-failed events and mock
  ERP succeeded and duplicate-skipped events carry only event, correlation, and
  source-customer identifiers plus the safe failure category where relevant.
  Log-capture tests verify those fields and prove that business IDs, customer
  names, billing cities, Kafka keys, and exception details are omitted.

## Current architecture

The target contains two separately runnable responsibilities—Salesforce
integration and mock ERP consumption. They will live as separate Maven modules
in this repository. Only the integration-service module will be created during
the first slice; the mock ERP module will be added when its roadmap slice
begins.

Initial technical baseline:

- Java 21
- Maven Wrapper
- Spring Boot 4.1.0, selected through Spring Initializr
- Spring Web
- Jackson
- Jakarta Validation
- Spotless with a shared Eclipse formatter profile

Kafka, PostgreSQL, JPA, Flyway, Testcontainers, and stub-server dependencies
must be added only when their roadmap slice begins.

## Definition of finished

The MVP is finished when:

- the documented happy path works from the chosen trigger through PostgreSQL;
- invalid ERP data produces a visible, recoverable failure rather than silent
  loss;
- replaying the same event does not create duplicate ERP customers;
- transient failures follow bounded retry and dead-letter rules;
- secrets are absent from source control and logs;
- focused and end-to-end tests pass reproducibly;
- local setup, architecture, operations, and limitations are documented;
- Maven verification and executable packaging succeed;
- portfolio claims describe only implemented behavior.

## Next task

Continue Milestone 8 with relevant application health information. Expose the
minimum useful readiness evidence for each service and distinguish application
liveness from dependency availability without revealing credentials, private
URLs, or other environment-specific configuration.

## Important decisions

### Confirmed

- The learner writes the implementation and receives mentoring, explanations,
  progressively stronger hints, debugging help, and senior review.
- Work proceeds through small vertical slices with explicit acceptance and exit
  criteria.
- Java 21 and Maven are the baseline.
- Salesforce access uses OAuth 2.0 Client Credentials.
- The integration direction is Salesforce to a fictional mock ERP.
- Salesforce credentials and environment-specific values are supplied outside
  source control.
- Live Salesforce access defaults to read-only designated test data.
- Kafka and PostgreSQL are postponed until the Java Salesforce client works
  reliably.
- The MVP includes no frontend, real SAP, or bidirectional synchronization.
- The project favors simplicity, explicit behavior, testability, and narrow
  completion over speculative enterprise abstractions.
- Synchronization is explicitly triggered with
  `POST /api/sync/account/{salesforceAccountId}`.
- The integration service and mock ERP will be separate Maven modules in one
  repository; modules will be introduced only when their roadmap slice begins.
- The initial safe Account response contains `salesforceAccountId`, `name`,
  `businessId`, and `billingCity`.
- The Salesforce Account business-ID field has the API name `Business_ID__c`.
- Salesforce HTTP communication will use Spring's synchronous `RestClient`.
- Token caching is deferred until repeated authenticated Salesforce requests
  make its lifecycle necessary; the current client requests a token per
  authentication call.
- OAuth server `5xx` responses use `SalesforceAuthenticationException` with
  their HTTP status, while connection failures use a separate
  `SalesforceAuthenticationUnavailableException` because no HTTP status exists.
- Salesforce OAuth will expose a separate immutable internal
  `SalesforceSession` for trusted Salesforce clients. It will contain the
  access token and instance URL without a generated token-leaking `toString()`,
  while `SalesforceAuthenticationResult` remains the safe representation.
- The OAuth client now creates validated internal Salesforce sessions while
  retaining its safe authentication-result method for callers that must not
  access credentials.
- Java formatting uses the repository's `Northstar` Eclipse profile in both
  Spotless and VS Code; `./mvnw spotless:check` verifies it and
  `./mvnw spotless:apply` fixes it.
- Mockito instrumentation is supplied explicitly to the Maven test JVM through
  the managed `mockito-core` artifact rather than dynamically attached at test
  runtime.
- The internal customer model will be Salesforce-independent. Its required
  fields are source customer ID, business ID, and name; billing city is
  optional. Mapping trims surrounding whitespace, converts a blank optional
  billing city to `null`, and preserves business-ID and name casing.
- Jakarta Bean Validation will validate the internal customer after mapping,
  with invalid data translated into a dedicated safe
  `CustomerValidationException`. The internal customer, Salesforce mapper,
  future Kafka contract, and future persistence entity remain separate types.
- A customer-preparation application service composes the tested Salesforce
  Account retrieval, normalization mapper, and Jakarta validation boundary. It
  returns a validated Salesforce-independent `Customer` or a safe validation
  exception identifying only invalid field names.
- Customer synchronization events will use the configurable topic
  `northstar.customer-sync.v1` by default. The versioned, self-describing event
  envelope will include event and correlation UUIDs, an `Instant` timestamp,
  event type, source system, version, and a customer payload separate from the
  internal domain model.
- Kafka messages will use `businessId` as their key so events for the same ERP
  customer retain partition ordering. This ordering key is separate from the
  event identity and the future consumer idempotency policy.
- The mock ERP idempotency key for the single-source MVP is Salesforce
  `sourceCustomerId`, enforced by a unique database constraint. `eventId` is not
  the customer idempotency key because repeated HTTP triggers create distinct
  events for the same source customer.
- Repeated customer synchronization uses update semantics: the existing ERP
  row keeps its database identity while business ID, name, and billing city are
  replaced by the latest valid event values.
- Existing duplicate rows are disposable development data and will be removed
  by explicitly resetting the local PostgreSQL volume before the unique
  constraint migration is applied; the migration itself will not silently
  delete duplicates.
- Successfully processed customer-sync events are recorded by unique `eventId`.
  Receipt presence means success and causes exact redelivery to be skipped; no
  `PROCESSING` or `FAILED` status is stored in this table. Failed transactions
  leave no receipt, and durable failure history is deferred to Milestone 7's
  retry and dead-letter design.
- Kafka consumer retry and recovery belong to the mock ERP. Validation failures
  are non-retryable; database/infrastructure and unexpected runtime failures are
  retryable for three total attempts with a fixed one-second backoff. Exhausted
  and non-retryable records go to the source topic plus `.DLT`, preserving the
  original record and safe diagnostic metadata without exposing stack traces or
  sensitive exception messages.
- Event time and UUID generation will be controllable in tests. After Kafka is
  connected, the HTTP trigger will return `202 Accepted` with safe event and
  correlation identifiers only after the broker acknowledges publication; a
  publication failure will produce a sanitized `503` response and will not
  claim that ERP synchronization completed.
- Synchronization audit evidence remains owned by the service that can state it
  truthfully. The integration service durably owns `INITIATED`, `PUBLISHED`,
  and `PUBLICATION_FAILED`; the mock ERP processing receipt proves `SUCCEEDED`;
  and a DLT record proves terminal `FAILED` delivery. `CONSUMED` is deliberately
  not durable because it is an intermediate observation that could become
  misleading after a crash. Audit evidence is connected by `eventId`,
  `correlationId`, and `sourceCustomerId`, and integration-owned records will be
  available through a safe lookup endpoint.
- Local development reuses one PostgreSQL server but gives the integration
  service its own `integration_service` database, tables, and Flyway history.
  The mock ERP retains its `mock_erp` database, and neither service may query
  the other's database. Spring Boot's typed datasource configuration supplies
  the integration-service connection; Testcontainers supplies isolated test
  databases.

### Pending

- How downstream success and DLT evidence will be exposed without coupling the
  integration service directly to the mock ERP database.

Record future decisions here with enough context to explain why they were made.
