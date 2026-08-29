package com.northstar.integrationservice.application.customer;

import org.springframework.stereotype.Service;

import com.northstar.integrationservice.domain.customer.Customer;
import com.northstar.integrationservice.messaging.customer.CustomerSyncEventFactory;
import com.northstar.integrationservice.messaging.customer.CustomerSyncEventPublisher;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationResult;
import com.northstar.integrationservice.messaging.customer.CustomerSyncRequestedEvent;

@Service
public class CustomerSynchronizationService {
    private final CustomerPreparationService customerPreparationService;
    private final CustomerSyncEventFactory eventFactory;
    private final CustomerSyncEventPublisher eventPublisher;

    public CustomerSynchronizationService(CustomerPreparationService customerPreparationService,
            CustomerSyncEventFactory eventFactory, CustomerSyncEventPublisher eventPublisher) {
        this.customerPreparationService = customerPreparationService;
        this.eventFactory = eventFactory;
        this.eventPublisher = eventPublisher;
    }

    public CustomerSyncPublicationResult synchronizeAccount(String salesforceAccountId) {
        Customer customer = this.customerPreparationService.prepareCustomer(salesforceAccountId);

        CustomerSyncRequestedEvent event = this.eventFactory.generateEvent(customer);

        return this.eventPublisher.publishEvent(event);
    }
}
