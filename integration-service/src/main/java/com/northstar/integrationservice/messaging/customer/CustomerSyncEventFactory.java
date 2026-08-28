package com.northstar.integrationservice.messaging.customer;

import java.time.Clock;
import java.time.Instant;

import com.northstar.integrationservice.domain.customer.Customer;

public class CustomerSyncEventFactory {
    private static final int EVENT_VERSION = 1;
    private static final String EVENT_TYPE = "CUSTOMER_SYNC_REQUESTED";
    private static final String SOURCE_SYSTEM = "SALESFORCE";

    private final Clock clock;
    private final UuidGenerator uuidGenerator;

    public CustomerSyncEventFactory(Clock clock, UuidGenerator uuidGenerator) {
        this.clock = clock;
        this.uuidGenerator = uuidGenerator;
    }

    public CustomerSyncRequestedEvent generateEvent(Customer customer) {
        CustomerSyncPayload payload = new CustomerSyncPayload(customer.sourceCustomerId(),
                customer.businessId(), customer.name(), customer.billingCity());

        return new CustomerSyncRequestedEvent(this.uuidGenerator.generate(),
                this.uuidGenerator.generate(), Instant.now(this.clock), EVENT_VERSION, EVENT_TYPE,
                SOURCE_SYSTEM, payload);
    }
}
