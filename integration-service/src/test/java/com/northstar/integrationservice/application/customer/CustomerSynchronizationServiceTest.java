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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.northstar.integrationservice.application.audit.CustomerSyncAuditService;
import com.northstar.integrationservice.domain.customer.Customer;
import com.northstar.integrationservice.domain.customer.CustomerValidationException;
import com.northstar.integrationservice.messaging.customer.CustomerSyncEventFactory;
import com.northstar.integrationservice.messaging.customer.CustomerSyncEventPublisher;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPayload;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationException;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationResult;
import com.northstar.integrationservice.messaging.customer.CustomerSyncRequestedEvent;

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

    @BeforeEach
    void setUp() {
        service = new CustomerSynchronizationService(customerPreparationService, eventFactory,
                eventPublisher, auditService);
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
}
