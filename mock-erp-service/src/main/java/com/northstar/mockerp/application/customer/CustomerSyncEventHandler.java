package com.northstar.mockerp.application.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.mockerp.domain.customer.ErpCustomer;
import com.northstar.mockerp.messaging.customer.CustomerSyncRequestedEvent;
import com.northstar.mockerp.persistence.customer.ErpCustomerEntity;
import com.northstar.mockerp.persistence.customer.ErpCustomerRepository;

@Component
public class CustomerSyncEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerSyncEventHandler.class);

    private final CustomerSyncPayloadToErpCustomerMapper customerMapper;
    private final ErpCustomerValidator validator;
    private final ErpCustomerToEntityMapper entityMapper;
    private final ErpCustomerRepository repository;

    public CustomerSyncEventHandler(CustomerSyncPayloadToErpCustomerMapper customerMapper,
            ErpCustomerValidator validator, ErpCustomerToEntityMapper entityMapper,
            ErpCustomerRepository repository) {
        this.customerMapper = customerMapper;
        this.validator = validator;
        this.entityMapper = entityMapper;
        this.repository = repository;
    }

    @Transactional
    public void handle(String messageKey, CustomerSyncRequestedEvent event) {
        ErpCustomer customer = customerMapper.map(event.customer());
        ErpCustomer validatedCustomer = validator.validate(customer);
        ErpCustomerEntity customerEntity = entityMapper.map(validatedCustomer);
        repository.save(customerEntity);

        LOGGER.info(
                "Received customer sync event: eventId={}, correlationId={}, businessId={}, messageKey={}",
                event.eventId(), event.correlationId(), customer.businessId(), messageKey);
    }
}
