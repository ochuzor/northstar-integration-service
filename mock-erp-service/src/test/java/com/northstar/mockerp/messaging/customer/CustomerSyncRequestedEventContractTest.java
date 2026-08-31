package com.northstar.mockerp.messaging.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class CustomerSyncRequestedEventContractTest {
    @Test
    void deserializesVersionOneCustomerSyncEvent() throws JacksonException {
        ObjectMapper objectMapper = JsonMapper.builder().build();

        String json = """
                {
                  "eventId": "11111111-1111-1111-1111-111111111111",
                  "correlationId": "22222222-2222-2222-2222-222222222222",
                  "occurredAt": "2026-08-28T10:15:30Z",
                  "eventVersion": 1,
                  "eventType": "CUSTOMER_SYNC_REQUESTED",
                  "sourceSystem": "SALESFORCE",
                  "customer": {
                    "sourceCustomerId": "001ABC123456789",
                    "businessId": "NORTHSTAR-001",
                    "name": "Designated Test Account",
                    "billingCity": "Helsinki"
                  }
                }
                """;

        CustomerSyncRequestedEvent event = objectMapper.readValue(json,
                CustomerSyncRequestedEvent.class);

        assertThat(event.eventId())
                .isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(event.correlationId())
                .isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-08-28T10:15:30Z"));
        assertThat(event.eventVersion()).isEqualTo(1);
        assertThat(event.eventType()).isEqualTo("CUSTOMER_SYNC_REQUESTED");
        assertThat(event.sourceSystem()).isEqualTo("SALESFORCE");
        assertThat(event.customer().sourceCustomerId()).isEqualTo("001ABC123456789");
        assertThat(event.customer().businessId()).isEqualTo("NORTHSTAR-001");
        assertThat(event.customer().name()).isEqualTo("Designated Test Account");
        assertThat(event.customer().billingCity()).isEqualTo("Helsinki");
    }
}
