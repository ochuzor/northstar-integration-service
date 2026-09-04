package com.northstar.integrationservice.application.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Service;

import com.northstar.integrationservice.application.audit.CustomerSyncAuditService;
import com.northstar.integrationservice.domain.audit.CustomerSyncAuditFailureCategory;
import com.northstar.integrationservice.domain.customer.Customer;
import com.northstar.integrationservice.messaging.customer.CustomerSyncEventFactory;
import com.northstar.integrationservice.messaging.customer.CustomerSyncEventPublisher;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationException;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationResult;
import com.northstar.integrationservice.messaging.customer.CustomerSyncRequestedEvent;

@Service
public class CustomerSynchronizationService {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(CustomerSynchronizationService.class);

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
        logLifecycleEvent(LOGGER.atInfo(), "customer_sync_initiated", event)
                .log("Customer synchronization initiated");

        CustomerSyncPublicationResult result;
        try {
            result = this.eventPublisher.publishEvent(event);
        } catch (CustomerSyncPublicationException exception) {
            this.auditService.markPublicationFailed(event.correlationId());
            logLifecycleEvent(LOGGER.atWarn(), "customer_sync_publication_failed", event)
                    .addKeyValue("failureCategory",
                            CustomerSyncAuditFailureCategory.KAFKA_PUBLICATION)
                    .log("Customer synchronization publication failed");
            throw exception;
        }

        this.auditService.markPublished(event.correlationId());
        logLifecycleEvent(LOGGER.atInfo(), "customer_sync_published", event)
                .log("Customer synchronization published");
        return result;
    }

    private LoggingEventBuilder logLifecycleEvent(LoggingEventBuilder logBuilder,
            String lifecycleEvent, CustomerSyncRequestedEvent event) {
        return logBuilder.addKeyValue("event", lifecycleEvent)
                .addKeyValue("eventId", event.eventId())
                .addKeyValue("correlationId", event.correlationId())
                .addKeyValue("sourceCustomerId", event.customer().sourceCustomerId());
    }
}
