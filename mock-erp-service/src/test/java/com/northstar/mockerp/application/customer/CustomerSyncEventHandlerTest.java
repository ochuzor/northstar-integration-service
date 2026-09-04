package com.northstar.mockerp.application.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import com.northstar.mockerp.domain.customer.ErpCustomer;
import com.northstar.mockerp.domain.customer.ErpCustomerValidationException;
import com.northstar.mockerp.messaging.customer.CustomerSyncPayload;
import com.northstar.mockerp.messaging.customer.CustomerSyncRequestedEvent;
import com.northstar.mockerp.persistence.customer.ErpCustomerEntity;
import com.northstar.mockerp.persistence.customer.ErpCustomerRepository;
import com.northstar.mockerp.persistence.customer.ErpProcessedCustomerSyncEventEntity;
import com.northstar.mockerp.persistence.customer.ErpProcessedCustomerSyncEventEntityRepository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class CustomerSyncEventHandlerTest {

    @Test
    void createsCustomerWhenSourceCustomerIdDoesNotExist() {
        CustomerSyncPayloadToErpCustomerMapper payloadMapper = mock(
                CustomerSyncPayloadToErpCustomerMapper.class);
        ErpCustomerValidator validator = mock(ErpCustomerValidator.class);
        ErpCustomerToEntityMapper entityMapper = mock(ErpCustomerToEntityMapper.class);
        ErpCustomerRepository repository = mock(ErpCustomerRepository.class);
        ErpProcessedCustomerSyncEventEntityRepository processedEventRepository = mock(
                ErpProcessedCustomerSyncEventEntityRepository.class);

        CustomerSyncEventHandler handler = new CustomerSyncEventHandler(payloadMapper, validator,
                entityMapper, repository, processedEventRepository, fixedClock());
        CustomerSyncPayload payload = new CustomerSyncPayload("customer-id", "business-id",
                "Customer Name", "Helsinki");
        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-08-28T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                payload);
        ErpCustomer customer = new ErpCustomer("customer-id", "business-id", "Customer Name",
                "Helsinki");
        ErpCustomerEntity entity = new ErpCustomerEntity("customer-id", "business-id",
                "Customer Name", "Helsinki");
        when(payloadMapper.map(payload)).thenReturn(customer);
        when(validator.validate(customer)).thenReturn(customer);
        when(repository.findBySourceCustomerId("customer-id")).thenReturn(Optional.empty());
        when(entityMapper.map(customer)).thenReturn(entity);

        handler.handle("business-id", event);

        InOrder processingOrder = inOrder(payloadMapper, validator, entityMapper, repository);
        processingOrder.verify(payloadMapper).map(payload);
        processingOrder.verify(validator).validate(customer);
        processingOrder.verify(repository).findBySourceCustomerId("customer-id");
        processingOrder.verify(entityMapper).map(customer);
        processingOrder.verify(repository).save(entity);
        verify(processedEventRepository).save(argThat(processedEvent -> processedEvent.getEventId()
                .equals(event.eventId())
                && processedEvent.getSourceCustomerId().equals("customer-id")
                && processedEvent.getProcessedAt().equals(Instant.parse("2026-09-04T08:00:00Z"))));
    }

    @Test
    void doesNotPersistInvalidCustomer() {
        CustomerSyncPayloadToErpCustomerMapper payloadMapper = mock(
                CustomerSyncPayloadToErpCustomerMapper.class);
        ErpCustomerValidator validator = mock(ErpCustomerValidator.class);
        ErpCustomerToEntityMapper entityMapper = mock(ErpCustomerToEntityMapper.class);
        ErpCustomerRepository repository = mock(ErpCustomerRepository.class);
        ErpProcessedCustomerSyncEventEntityRepository processedEventRepository = mock(
                ErpProcessedCustomerSyncEventEntityRepository.class);

        CustomerSyncEventHandler handler = new CustomerSyncEventHandler(payloadMapper, validator,
                entityMapper, repository, processedEventRepository, fixedClock());
        CustomerSyncPayload payload = new CustomerSyncPayload("customer-id", null, "Customer Name",
                null);
        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-08-28T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                payload);
        ErpCustomer customer = new ErpCustomer("customer-id", null, "Customer Name", null);
        ErpCustomerValidationException validationFailure = new ErpCustomerValidationException(
                Set.of("businessId"));
        when(payloadMapper.map(payload)).thenReturn(customer);
        when(validator.validate(customer)).thenThrow(validationFailure);

        assertThatThrownBy(() -> handler.handle(null, event)).isSameAs(validationFailure);

        verifyNoInteractions(entityMapper, repository);
    }

    @Test
    void propagatesPersistenceFailure() {
        CustomerSyncPayloadToErpCustomerMapper payloadMapper = mock(
                CustomerSyncPayloadToErpCustomerMapper.class);
        ErpCustomerValidator validator = mock(ErpCustomerValidator.class);
        ErpCustomerToEntityMapper entityMapper = mock(ErpCustomerToEntityMapper.class);
        ErpCustomerRepository repository = mock(ErpCustomerRepository.class);
        ErpProcessedCustomerSyncEventEntityRepository processedEventRepository = mock(
                ErpProcessedCustomerSyncEventEntityRepository.class);
        CustomerSyncEventHandler handler = new CustomerSyncEventHandler(payloadMapper, validator,
                entityMapper, repository, processedEventRepository, fixedClock());
        CustomerSyncPayload payload = new CustomerSyncPayload("customer-id", "business-id",
                "Customer Name", "Helsinki");
        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-08-28T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                payload);
        ErpCustomer customer = new ErpCustomer("customer-id", "business-id", "Customer Name",
                "Helsinki");
        ErpCustomerEntity entity = new ErpCustomerEntity("customer-id", "business-id",
                "Customer Name", "Helsinki");
        DataAccessResourceFailureException persistenceFailure = new DataAccessResourceFailureException(
                "database unavailable");
        when(payloadMapper.map(payload)).thenReturn(customer);
        when(validator.validate(customer)).thenReturn(customer);
        when(repository.findBySourceCustomerId("customer-id")).thenReturn(Optional.empty());
        when(entityMapper.map(customer)).thenReturn(entity);
        when(repository.save(entity)).thenThrow(persistenceFailure);

        assertThatThrownBy(() -> handler.handle("business-id", event)).isSameAs(persistenceFailure);
        verify(processedEventRepository, never())
                .save(any(ErpProcessedCustomerSyncEventEntity.class));
    }

    @Test
    void updatesExistingCustomerWhenSourceCustomerIdAlreadyExists() {
        CustomerSyncPayloadToErpCustomerMapper payloadMapper = mock(
                CustomerSyncPayloadToErpCustomerMapper.class);
        ErpCustomerValidator validator = mock(ErpCustomerValidator.class);
        ErpCustomerToEntityMapper entityMapper = mock(ErpCustomerToEntityMapper.class);
        ErpCustomerRepository repository = mock(ErpCustomerRepository.class);
        ErpProcessedCustomerSyncEventEntityRepository processedEventRepository = mock(
                ErpProcessedCustomerSyncEventEntityRepository.class);
        CustomerSyncEventHandler handler = new CustomerSyncEventHandler(payloadMapper, validator,
                entityMapper, repository, processedEventRepository, fixedClock());
        CustomerSyncPayload payload = new CustomerSyncPayload("customer-id", "new-business-id",
                "New Customer Name", "Espoo");
        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-08-28T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                payload);
        ErpCustomer customer = new ErpCustomer("customer-id", "new-business-id",
                "New Customer Name", "Espoo");
        ErpCustomerEntity existingEntity = new ErpCustomerEntity("customer-id", "old-business-id",
                "Old Customer Name", "Helsinki");
        when(payloadMapper.map(payload)).thenReturn(customer);
        when(validator.validate(customer)).thenReturn(customer);
        when(repository.findBySourceCustomerId("customer-id"))
                .thenReturn(Optional.of(existingEntity));

        handler.handle("new-business-id", event);

        assertThat(existingEntity.getSourceCustomerId()).isEqualTo("customer-id");
        assertThat(existingEntity.getBusinessId()).isEqualTo("new-business-id");
        assertThat(existingEntity.getName()).isEqualTo("New Customer Name");
        assertThat(existingEntity.getBillingCity()).isEqualTo("Espoo");
        verify(entityMapper, never()).map(customer);
        verify(repository).save(existingEntity);
    }

    @Test
    void skipsEventWhenEventIdHasAlreadyBeenRecorded() {
        CustomerSyncPayloadToErpCustomerMapper payloadMapper = mock(
                CustomerSyncPayloadToErpCustomerMapper.class);
        ErpCustomerValidator validator = mock(ErpCustomerValidator.class);
        ErpCustomerToEntityMapper entityMapper = mock(ErpCustomerToEntityMapper.class);
        ErpCustomerRepository repository = mock(ErpCustomerRepository.class);
        ErpProcessedCustomerSyncEventEntityRepository processedEventRepository = mock(
                ErpProcessedCustomerSyncEventEntityRepository.class);
        CustomerSyncEventHandler handler = new CustomerSyncEventHandler(payloadMapper, validator,
                entityMapper, repository, processedEventRepository, fixedClock());
        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(eventId,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-08-28T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                new CustomerSyncPayload("customer-id", "business-id", "Customer Name", "Helsinki"));
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        handler.handle("business-id", event);

        verify(processedEventRepository).existsById(eventId);
        verify(processedEventRepository, never())
                .save(any(ErpProcessedCustomerSyncEventEntity.class));
        verifyNoInteractions(payloadMapper, validator, entityMapper, repository);
    }

    @Test
    void logsSuccessfulCustomerProcessingWithoutPayload() {
        CustomerSyncPayloadToErpCustomerMapper payloadMapper = mock(
                CustomerSyncPayloadToErpCustomerMapper.class);
        ErpCustomerValidator validator = mock(ErpCustomerValidator.class);
        ErpCustomerToEntityMapper entityMapper = mock(ErpCustomerToEntityMapper.class);
        ErpCustomerRepository repository = mock(ErpCustomerRepository.class);
        ErpProcessedCustomerSyncEventEntityRepository processedEventRepository = mock(
                ErpProcessedCustomerSyncEventEntityRepository.class);
        CustomerSyncEventHandler handler = new CustomerSyncEventHandler(payloadMapper, validator,
                entityMapper, repository, processedEventRepository, fixedClock());
        CustomerSyncRequestedEvent event = event();
        ErpCustomer customer = new ErpCustomer("customer-id", "sensitive-business-id",
                "Sensitive Customer Name", "Sensitive City");
        ErpCustomerEntity entity = new ErpCustomerEntity("customer-id", "sensitive-business-id",
                "Sensitive Customer Name", "Sensitive City");
        when(payloadMapper.map(event.customer())).thenReturn(customer);
        when(validator.validate(customer)).thenReturn(customer);
        when(repository.findBySourceCustomerId("customer-id")).thenReturn(Optional.empty());
        when(entityMapper.map(customer)).thenReturn(entity);
        ListAppender<ILoggingEvent> appender = attachLogAppender();

        try {
            handler.handle("sensitive-message-key", event);

            ILoggingEvent succeeded = lifecycleLog(appender, "customer_sync_succeeded");
            assertThat(succeeded.getLevel()).isEqualTo(Level.INFO);
            assertLifecycleIdentifiers(succeeded, event);
            assertThat(logOutput(appender)).doesNotContain("sensitive-business-id",
                    "sensitive-message-key", "Sensitive Customer Name", "Sensitive City");
        } finally {
            detachLogAppender(appender);
        }
    }

    @Test
    void logsDuplicateEventAsSkipped() {
        CustomerSyncPayloadToErpCustomerMapper payloadMapper = mock(
                CustomerSyncPayloadToErpCustomerMapper.class);
        ErpCustomerValidator validator = mock(ErpCustomerValidator.class);
        ErpCustomerToEntityMapper entityMapper = mock(ErpCustomerToEntityMapper.class);
        ErpCustomerRepository repository = mock(ErpCustomerRepository.class);
        ErpProcessedCustomerSyncEventEntityRepository processedEventRepository = mock(
                ErpProcessedCustomerSyncEventEntityRepository.class);
        CustomerSyncEventHandler handler = new CustomerSyncEventHandler(payloadMapper, validator,
                entityMapper, repository, processedEventRepository, fixedClock());
        CustomerSyncRequestedEvent event = event();
        when(processedEventRepository.existsById(event.eventId())).thenReturn(true);
        ListAppender<ILoggingEvent> appender = attachLogAppender();

        try {
            handler.handle("sensitive-message-key", event);

            ILoggingEvent skipped = lifecycleLog(appender, "customer_sync_duplicate_skipped");
            assertThat(skipped.getLevel()).isEqualTo(Level.INFO);
            assertLifecycleIdentifiers(skipped, event);
            assertThat(appender.list)
                    .noneSatisfy(logEvent -> assertThat(logValue(logEvent, "event"))
                            .isEqualTo("customer_sync_succeeded"));
            assertThat(logOutput(appender)).doesNotContain("sensitive-business-id",
                    "sensitive-message-key", "Sensitive Customer Name", "Sensitive City");
        } finally {
            detachLogAppender(appender);
        }
    }

    private CustomerSyncRequestedEvent event() {
        return new CustomerSyncRequestedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-08-28T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                new CustomerSyncPayload("customer-id", "sensitive-business-id",
                        "Sensitive Customer Name", "Sensitive City"));
    }

    private ListAppender<ILoggingEvent> attachLogAppender() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(CustomerSyncEventHandler.class)).addAppender(appender);
        return appender;
    }

    private void detachLogAppender(ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(CustomerSyncEventHandler.class)).detachAppender(appender);
        appender.stop();
    }

    private ILoggingEvent lifecycleLog(ListAppender<ILoggingEvent> appender,
            String lifecycleEvent) {
        return appender.list.stream()
                .filter(logEvent -> lifecycleEvent.equals(logValue(logEvent, "event"))).findFirst()
                .orElseThrow();
    }

    private void assertLifecycleIdentifiers(ILoggingEvent logEvent,
            CustomerSyncRequestedEvent event) {
        assertThat(logValue(logEvent, "eventId")).isEqualTo(event.eventId().toString());
        assertThat(logValue(logEvent, "correlationId")).isEqualTo(event.correlationId().toString());
        assertThat(logValue(logEvent, "sourceCustomerId"))
                .isEqualTo(event.customer().sourceCustomerId());
    }

    private String logValue(ILoggingEvent logEvent, String key) {
        return logEvent.getKeyValuePairs().stream().filter(pair -> pair.key.equals(key))
                .map(pair -> String.valueOf(pair.value)).findFirst().orElse(null);
    }

    private String logOutput(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream().map(logEvent -> logEvent.getFormattedMessage()
                + String.valueOf(logEvent.getKeyValuePairs())).reduce("", String::concat);
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-09-04T08:00:00Z"), ZoneOffset.UTC);
    }
}
