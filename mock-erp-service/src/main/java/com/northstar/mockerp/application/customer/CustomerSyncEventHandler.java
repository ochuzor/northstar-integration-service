package com.northstar.mockerp.application.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.northstar.mockerp.domain.customer.ErpCustomer;
import com.northstar.mockerp.messaging.customer.CustomerSyncRequestedEvent;

@Component
public class CustomerSyncEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerSyncEventHandler.class);

    private final CustomerSyncPayloadToErpCustomerMapper mapper;
    private final ErpCustomerValidator validator;

    public CustomerSyncEventHandler(CustomerSyncPayloadToErpCustomerMapper mapper,
            ErpCustomerValidator validator) {
        this.mapper = mapper;
        this.validator = validator;
    }

    public void handle(String messageKey, CustomerSyncRequestedEvent event) {
        ErpCustomer customer = mapper.map(event.customer());
        validator.validate(customer);

        LOGGER.info(
                "Received customer sync event: eventId={}, correlationId={}, businessId={}, messageKey={}",
                event.eventId(), event.correlationId(), customer.businessId(), messageKey);
    }
}
