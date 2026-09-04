package com.northstar.integrationservice.application.customer;

import org.springframework.stereotype.Service;

import com.northstar.integrationservice.application.audit.CustomerSyncAuditService;
import com.northstar.integrationservice.domain.customer.Customer;
import com.northstar.integrationservice.messaging.customer.CustomerSyncEventFactory;
import com.northstar.integrationservice.messaging.customer.CustomerSyncEventPublisher;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationException;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationResult;
import com.northstar.integrationservice.messaging.customer.CustomerSyncRequestedEvent;

@Service
public class CustomerSynchronizationService {
    private final CustomerPreparationService customerPreparationService;
    private final CustomerSyncEventFactory eventFactory;
    private final CustomerSyncEventPublisher eventPublisher;
    private final CustomerSyncAuditService auditService;

    public CustomerSynchronizationService(CustomerPreparationService customerPreparationService,
            CustomerSyncEventFactory eventFactory, CustomerSyncEventPublisher eventPublisher,
            CustomerSyncAuditService auditService) {
        this.customerPreparationService = customerPreparationService;
        this.eventFactory = eventFactory;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    public CustomerSyncPublicationResult synchronizeAccount(String salesforceAccountId) {
        Customer customer = this.customerPreparationService.prepareCustomer(salesforceAccountId);

        CustomerSyncRequestedEvent event = this.eventFactory.generateEvent(customer);
        this.auditService.recordInitiated(event);

        CustomerSyncPublicationResult result;
        try {
            result = this.eventPublisher.publishEvent(event);
        } catch (CustomerSyncPublicationException exception) {
            this.auditService.markPublicationFailed(event.correlationId());
            throw exception;
        }

        this.auditService.markPublished(event.correlationId());
        return result;
    }
}
