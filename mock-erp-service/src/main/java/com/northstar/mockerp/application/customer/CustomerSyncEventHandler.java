package com.northstar.mockerp.application.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.northstar.mockerp.messaging.customer.CustomerSyncRequestedEvent;

@Component
public class CustomerSyncEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerSyncEventHandler.class);

    public void handle(String messageKey, CustomerSyncRequestedEvent event) {
        LOGGER.info(
                "Received customer sync event: eventId={}, correlationId={}, businessId={}, messageKey={}",
                event.eventId(), event.correlationId(), event.customer().businessId(), messageKey);
    }
}
