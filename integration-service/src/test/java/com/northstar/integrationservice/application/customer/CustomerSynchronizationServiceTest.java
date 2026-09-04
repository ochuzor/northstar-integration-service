package com.northstar.integrationservice.application.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.northstar.integrationservice.application.audit.CustomerSyncAuditService;
import com.northstar.integrationservice.domain.customer.Customer;
import com.northstar.integrationservice.domain.customer.CustomerValidationException;
import com.northstar.integrationservice.messaging.customer.CustomerSyncEventFactory;
import com.northstar.integrationservice.messaging.customer.CustomerSyncEventPublisher;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPayload;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationException;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationResult;
import com.northstar.integrationservice.messaging.customer.CustomerSyncRequestedEvent;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class CustomerSynchronizationServiceTest {
    @Mock
    private CustomerPreparationService customerPreparationService;

    @Mock
    private CustomerSyncEventFactory eventFactory;

    @Mock
    private CustomerSyncEventPublisher eventPublisher;

    @Mock
    private CustomerSyncAuditService auditService;

    private CustomerSynchronizationService service;
    private Logger serviceLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        serviceLogger = (Logger) LoggerFactory.getLogger(CustomerSynchronizationService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);
        service = new CustomerSynchronizationService(customerPreparationService, eventFactory,
                eventPublisher, auditService);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void synchronizesPreparedCustomerThroughKafka() {
        String salesforceAccountId = "001ABC123456789";
        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID correlationId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Customer customer = new Customer(salesforceAccountId, "NORTHSTAR-001",
                "Designated Test Account", "Helsinki");
        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(eventId, correlationId,
                Instant.parse("2026-08-29T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                new CustomerSyncPayload(salesforceAccountId, "NORTHSTAR-001",
                        "Designated Test Account", "Helsinki"));
        CustomerSyncPublicationResult expectedResult = new CustomerSyncPublicationResult(eventId,
                correlationId, salesforceAccountId);

        when(customerPreparationService.prepareCustomer(salesforceAccountId)).thenReturn(customer);
        when(eventFactory.generateEvent(customer)).thenReturn(event);
        when(eventPublisher.publishEvent(event)).thenReturn(expectedResult);

        CustomerSyncPublicationResult result = service.synchronizeAccount(salesforceAccountId);

        assertThat(result).isSameAs(expectedResult);
        verify(customerPreparationService).prepareCustomer(salesforceAccountId);
        verify(eventFactory).generateEvent(customer);
        InOrder publicationOrder = inOrder(auditService, eventPublisher);
        publicationOrder.verify(auditService).recordInitiated(event);
        publicationOrder.verify(eventPublisher).publishEvent(event);
        publicationOrder.verify(auditService).markPublished(correlationId);
    }

    @Test
    void doesNotCreateOrPublishEventWhenCustomerPreparationFails() {
        String salesforceAccountId = "001ABC123456789";
        CustomerValidationException preparationFailure = new CustomerValidationException(
                Set.of("businessId"));

        when(customerPreparationService.prepareCustomer(salesforceAccountId))
                .thenThrow(preparationFailure);

        assertThatThrownBy(() -> service.synchronizeAccount(salesforceAccountId))
                .isSameAs(preparationFailure);

        verify(customerPreparationService).prepareCustomer(salesforceAccountId);
        verifyNoInteractions(eventFactory, eventPublisher, auditService);
    }

    @Test
    void recordsPublicationFailureAndRethrowsSanitizedException() {
        String salesforceAccountId = "001ABC123456789";
        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID correlationId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Customer customer = new Customer(salesforceAccountId, "NORTHSTAR-001",
                "Designated Test Account", "Helsinki");
        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(eventId, correlationId,
                Instant.parse("2026-08-29T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                new CustomerSyncPayload(salesforceAccountId, "NORTHSTAR-001",
                        "Designated Test Account", "Helsinki"));
        CustomerSyncPublicationException publicationFailure = new CustomerSyncPublicationException();

        when(customerPreparationService.prepareCustomer(salesforceAccountId)).thenReturn(customer);
        when(eventFactory.generateEvent(customer)).thenReturn(event);
        when(eventPublisher.publishEvent(event)).thenThrow(publicationFailure);

        assertThatThrownBy(() -> service.synchronizeAccount(salesforceAccountId))
                .isSameAs(publicationFailure);

        InOrder publicationOrder = inOrder(auditService, eventPublisher);
        publicationOrder.verify(auditService).recordInitiated(event);
        publicationOrder.verify(eventPublisher).publishEvent(event);
        publicationOrder.verify(auditService).markPublicationFailed(correlationId);
    }

    @Test
    void logsSafeLifecycleIdentifiersWhenPublicationSucceeds() {
        CustomerSyncRequestedEvent event = event();
        Customer customer = new Customer(event.customer().sourceCustomerId(), "NORTHSTAR-001",
                "Sensitive Customer Name", "Sensitive City");
        CustomerSyncPublicationResult result = new CustomerSyncPublicationResult(event.eventId(),
                event.correlationId(), event.customer().sourceCustomerId());
        when(customerPreparationService.prepareCustomer(event.customer().sourceCustomerId()))
                .thenReturn(customer);
        when(eventFactory.generateEvent(customer)).thenReturn(event);
        when(eventPublisher.publishEvent(event)).thenReturn(result);

        service.synchronizeAccount(event.customer().sourceCustomerId());

        ILoggingEvent initiated = lifecycleLog("customer_sync_initiated");
        ILoggingEvent published = lifecycleLog("customer_sync_published");
        assertThat(initiated.getLevel()).isEqualTo(Level.INFO);
        assertThat(published.getLevel()).isEqualTo(Level.INFO);
        assertLifecycleIdentifiers(initiated, event);
        assertLifecycleIdentifiers(published, event);
        assertThat(logOutput()).doesNotContain("Sensitive Customer Name", "Sensitive City",
                "NORTHSTAR-001");
    }

    @Test
    void logsSafeFailureCategoryWithoutExceptionDetails() {
        CustomerSyncRequestedEvent event = event();
        Customer customer = new Customer(event.customer().sourceCustomerId(), "NORTHSTAR-001",
                "Sensitive Customer Name", "Sensitive City");
        CustomerSyncPublicationException failure = new CustomerSyncPublicationException();
        failure.initCause(new IllegalStateException("sensitive broker failure detail"));
        when(customerPreparationService.prepareCustomer(event.customer().sourceCustomerId()))
                .thenReturn(customer);
        when(eventFactory.generateEvent(customer)).thenReturn(event);
        when(eventPublisher.publishEvent(event)).thenThrow(failure);

        assertThatThrownBy(() -> service.synchronizeAccount(event.customer().sourceCustomerId()))
                .isSameAs(failure);

        ILoggingEvent failed = lifecycleLog("customer_sync_publication_failed");
        assertThat(failed.getLevel()).isEqualTo(Level.WARN);
        assertLifecycleIdentifiers(failed, event);
        assertThat(logValue(failed, "failureCategory")).isEqualTo("KAFKA_PUBLICATION");
        assertThat(logOutput()).doesNotContain("sensitive broker failure detail",
                "Sensitive Customer Name", "Sensitive City", "NORTHSTAR-001");
    }

    private CustomerSyncRequestedEvent event() {
        return new CustomerSyncRequestedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-08-29T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                new CustomerSyncPayload("001ABC123456789", "NORTHSTAR-001",
                        "Sensitive Customer Name", "Sensitive City"));
    }

    private ILoggingEvent lifecycleLog(String lifecycleEvent) {
        return logAppender.list.stream()
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

    private String logOutput() {
        return logAppender.list.stream().map(logEvent -> logEvent.getFormattedMessage()
                + String.valueOf(logEvent.getKeyValuePairs())).reduce("", String::concat);
    }
}
