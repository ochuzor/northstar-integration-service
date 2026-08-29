package com.northstar.integrationservice.messaging.customer;

public class CustomerSyncPublicationException extends RuntimeException {
    public CustomerSyncPublicationException() {
        super("Customer sync event publication failed");
    }
}
