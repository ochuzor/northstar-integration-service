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

The exact trigger for a synchronization request is not yet decided. The first
design task must choose a small explicit trigger rather than implying real-time
Salesforce change capture that the project has not implemented.

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
- Spotless enforces the committed Eclipse formatter profile during Maven
  verification, and committed VS Code settings use the same profile on save.

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

Continue Milestone 1 by defining the Salesforce token response contract and
implementing the dedicated `RestClient` token-client boundary.

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
- Java formatting uses the repository's `Northstar` Eclipse profile in both
  Spotless and VS Code; `./mvnw spotless:check` verifies it and
  `./mvnw spotless:apply` fixes it.

### Pending

- HTTP client choice and token caching lifecycle.
- Kafka topic and event-envelope contract.
- Idempotency key and transaction boundary.
- Retry ownership, retry limits, and dead-letter recovery workflow.
- Audit model and success/failure semantics.

Record future decisions here with enough context to explain why they were made.
