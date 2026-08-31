package com.northstar.mockerp.messaging.customer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import com.northstar.mockerp.application.customer.CustomerSyncEventHandler;

class CustomerSyncEventListenerTest {
    @Test
    void delegatesCustomerSyncEventToHandler() {
        CustomerSyncEventHandler eventHandler = mock(CustomerSyncEventHandler.class);
        CustomerSyncEventListener listener = new CustomerSyncEventListener(eventHandler);
        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-08-28T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                new CustomerSyncPayload("001ABC123456789", "NORTHSTAR-001",
                        "Designated Test Account", "Helsinki"));
        ConsumerRecord<String, CustomerSyncRequestedEvent> record = new ConsumerRecord<>(
                "northstar.customer-sync.v1", 0, 0, "NORTHSTAR-001", event);

        listener.consume(record);

        verify(eventHandler).handle("NORTHSTAR-001", event);
    }
}
