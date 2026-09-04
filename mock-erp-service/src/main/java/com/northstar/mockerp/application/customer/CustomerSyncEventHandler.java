package com.northstar.mockerp.application.customer;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.mockerp.domain.customer.ErpCustomer;
import com.northstar.mockerp.messaging.customer.CustomerSyncRequestedEvent;
import com.northstar.mockerp.persistence.customer.ErpCustomerEntity;
import com.northstar.mockerp.persistence.customer.ErpCustomerRepository;
import com.northstar.mockerp.persistence.customer.ErpProcessedCustomerSyncEventEntity;
import com.northstar.mockerp.persistence.customer.ErpProcessedCustomerSyncEventEntityRepository;

@Component
public class CustomerSyncEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerSyncEventHandler.class);

    private final CustomerSyncPayloadToErpCustomerMapper customerMapper;
    private final ErpCustomerValidator validator;
    private final ErpCustomerToEntityMapper entityMapper;
    private final ErpCustomerRepository customerRepository;
    private final ErpProcessedCustomerSyncEventEntityRepository syncEventEntityRepository;
    private final Clock clock;

    public CustomerSyncEventHandler(CustomerSyncPayloadToErpCustomerMapper customerMapper,
            ErpCustomerValidator validator, ErpCustomerToEntityMapper entityMapper,
            ErpCustomerRepository customerRepository,
            ErpProcessedCustomerSyncEventEntityRepository syncEventEntityRepository, Clock clock) {
        this.customerMapper = customerMapper;
        this.validator = validator;
        this.entityMapper = entityMapper;
        this.customerRepository = customerRepository;
        this.syncEventEntityRepository = syncEventEntityRepository;
        this.clock = clock;
    }

    @Transactional
    public void handle(String messageKey, CustomerSyncRequestedEvent event) {
        if (syncEventEntityRepository.existsById(event.eventId())) {
            logLifecycleEvent("customer_sync_duplicate_skipped", event)
                    .log("Customer synchronization duplicate skipped");
            return;
        }

        ErpCustomer customer = customerMapper.map(event.customer());
        ErpCustomer validatedCustomer = validator.validate(customer);

        ErpCustomerEntity customerEntity = customerRepository
                .findBySourceCustomerId(validatedCustomer.sourceCustomerId())
                .map(existingCustomer -> updateExistingCustomer(existingCustomer,
                        validatedCustomer))
                .orElseGet(() -> entityMapper.map(validatedCustomer));
        customerRepository.save(customerEntity);

        syncEventEntityRepository.save(new ErpProcessedCustomerSyncEventEntity(event.eventId(),
                validatedCustomer.sourceCustomerId(), Instant.now(clock)));

        logLifecycleEvent("customer_sync_succeeded", event)
                .log("Customer synchronization succeeded");
    }

    private LoggingEventBuilder logLifecycleEvent(String lifecycleEvent,
            CustomerSyncRequestedEvent event) {
        return LOGGER.atInfo().addKeyValue("event", lifecycleEvent)
                .addKeyValue("eventId", event.eventId())
                .addKeyValue("correlationId", event.correlationId())
                .addKeyValue("sourceCustomerId", event.customer().sourceCustomerId());
    }

    private ErpCustomerEntity updateExistingCustomer(ErpCustomerEntity existingCustomer,
            ErpCustomer customer) {
        existingCustomer.updateDetails(customer.businessId(), customer.name(),
                customer.billingCity());
        return existingCustomer;
    }
}
