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

Continue Milestone 4 by adding Spring Kafka, typed topic configuration, and the
production event-factory dependencies before implementing the producer
boundary.

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
- Event time and UUID generation will be controllable in tests. After Kafka is
  connected, the HTTP trigger will return `202 Accepted` with safe event and
  correlation identifiers only after the broker acknowledges publication; a
  publication failure will produce a sanitized `503` response and will not
  claim that ERP synchronization completed.

### Pending

- Idempotency key and transaction boundary.
- Retry ownership, retry limits, and dead-letter recovery workflow.
- Audit model and success/failure semantics.

Record future decisions here with enough context to explain why they were made.
