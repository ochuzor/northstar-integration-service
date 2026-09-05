# Customer synchronization observability

## Evidence model

Customer synchronization crosses two services, Kafka, and two independently
owned databases. It is therefore not one distributed database transaction and
does not have one global status row. Operators correlate the evidence below
using `eventId`, `correlationId`, and `sourceCustomerId`.

- The integration-service audit records the attempt through Kafka publication.
- The mock ERP processing receipt proves that one event committed successfully.
- A dead-letter record preserves a terminal consumer failure for diagnosis and
  deliberate replay.
- Structured lifecycle logs connect these durable records without exposing
  customer payloads or exception details.

`CONSUMED` is deliberately not persisted. A consumer could crash after writing
that state but before completing its database transaction, leaving misleading
evidence. Absence of a processing receipt means success has not been proven.

## Outcome matrix

| Scenario | Observable evidence | Automated proof |
| --- | --- | --- |
| Invalid Salesforce customer | The trigger returns `422`; event creation, audit recording, and Kafka publication do not occur. | `AccountSyncControllerTest.returnsUnprocessableEntityForInvalidCustomer`; `CustomerSynchronizationServiceTest.doesNotCreateOrPublishEventWhenCustomerPreparationFails` |
| Kafka publication succeeds | The integration audit transitions from `INITIATED` to `PUBLISHED`; the trigger returns an accepted receipt; correlated initiated and published logs are emitted. `GET /api/sync/{correlationId}` exposes the safe audit fields. | `CustomerSyncAuditServiceTest.recordsInitiatedAuditAtCurrentTime`; `CustomerSynchronizationAuditIntegrationTest.persistsPublishedAuditAfterBrokerAcknowledgement`; `CustomerSynchronizationServiceTest.logsSafeLifecycleIdentifiersWhenPublicationSucceeds`; `CustomerSyncAuditQueryIntegrationTest.returnsPersistedAuditByCorrelationId` |
| Kafka publication fails | The audit ends as `PUBLICATION_FAILED` with category `KAFKA_PUBLICATION`; the trigger returns `503`; a safe correlated warning is emitted before the sanitized exception is rethrown. | `CustomerSynchronizationAuditIntegrationTest.persistsPublicationFailureAndRethrowsSanitizedException`; `AccountSyncControllerTest.returnsServiceUnavailableWhenEventPublicationFails`; `CustomerSynchronizationServiceTest.logsSafeFailureCategoryWithoutExceptionDetails` |
| ERP processing succeeds | The customer row and event receipt commit together, and a correlated `customer_sync_succeeded` log is emitted. | `CustomerSyncKafkaToDatabaseIntegrationTest.consumesCustomerSyncEventAndPersistsCustomer`; `CustomerSyncEventHandlerTest.logsSuccessfulCustomerProcessingWithoutPayload` |
| Exact event is redelivered | The existing event receipt causes the duplicate to be skipped; customer and receipt counts stay at one; a correlated `customer_sync_duplicate_skipped` log distinguishes the outcome from success. | `CustomerSyncKafkaToDatabaseIntegrationTest.skipsExactDuplicateEventWithoutRepeatingPersistence`; `CustomerSyncEventHandlerTest.skipsEventWhenEventIdHasAlreadyBeenRecorded`; `CustomerSyncEventHandlerTest.logsDuplicateEventAsSkipped` |
| Temporary failure recovers | The handler succeeds on its second attempt, producing one customer and one receipt and no DLT record. | `CustomerSyncKafkaFailureIntegrationTest.retriesTemporaryFailureAndPersistsCustomerWithoutDeadLettering` |
| ERP validation fails | The handler runs once, no customer or receipt is committed, and the preserved DLT record carries only failure category `VALIDATION`. | `CustomerSyncKafkaFailureIntegrationTest.routesPermanentValidationFailureToDeadLetterTopicWithoutRetrying` |
| Retryable failure is exhausted | The handler runs three times, no customer or receipt is committed, and the preserved DLT record carries only failure category `RETRY_EXHAUSTED`. | `CustomerSyncKafkaFailureIntegrationTest.routesExhaustedRetryableFailureToDeadLetterTopicAfterThreeAttempts` |

## Operator interpretation

- `PUBLISHED` proves broker acknowledgement, not ERP completion.
- A processing receipt or `customer_sync_succeeded` log proves ERP success.
- A duplicate-skipped log means the same `eventId` was already committed.
- A DLT record is terminal failed evidence until an operator diagnoses and
  deliberately replays it using the procedure in `OPERATIONS.md`.
- If publication is recorded but neither ERP success nor a DLT record is yet
  visible, inspect service readiness and correlated lifecycle logs before
  taking recovery action.
