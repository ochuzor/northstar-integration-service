package com.northstar.integrationservice.messaging.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.northstar.integrationservice.domain.customer.Customer;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class CustomerSyncEventFactoryTest {
    @Test
    void createsVersionedCustomerSyncEvent() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-28T10:15:30Z"), ZoneOffset.UTC);

        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID correlationId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Deque<UUID> ids = new ArrayDeque<>(List.of(eventId, correlationId));
        UuidGenerator uuidGenerator = ids::removeFirst;

        CustomerSyncEventFactory eventFactory = new CustomerSyncEventFactory(clock, uuidGenerator);

        Customer customer = new Customer("customer-id", "business-id", "customer name", "Memphis");
        CustomerSyncRequestedEvent requestedEvent = eventFactory.generateEvent(customer);

        assertThat(requestedEvent.eventId()).isEqualTo(eventId);
        assertThat(requestedEvent.correlationId()).isEqualTo(correlationId);
        assertThat(requestedEvent.occurredAt()).isEqualTo(Instant.parse("2026-08-28T10:15:30Z"));
        assertThat(requestedEvent.eventVersion()).isEqualTo(1);
        assertThat(requestedEvent.eventType()).isEqualTo("CUSTOMER_SYNC_REQUESTED");
        assertThat(requestedEvent.sourceSystem()).isEqualTo("SALESFORCE");

        assertThat(requestedEvent.customer().sourceCustomerId())
                .isEqualTo(customer.sourceCustomerId());
        assertThat(requestedEvent.customer().businessId()).isEqualTo(customer.businessId());
        assertThat(requestedEvent.customer().name()).isEqualTo(customer.name());
        assertThat(requestedEvent.customer().billingCity()).isEqualTo(customer.billingCity());
    }

    @Test
    void usesProvidedClockForOccurredAt() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-28T10:15:30Z"), ZoneOffset.UTC);

        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID correlationId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Deque<UUID> ids = new ArrayDeque<>(List.of(eventId, correlationId));
        UuidGenerator uuidGenerator = ids::removeFirst;

        CustomerSyncEventFactory eventFactory = new CustomerSyncEventFactory(clock, uuidGenerator);

        Customer customer = new Customer("customer-id", "business-id", "customer name", "Memphis");
        CustomerSyncRequestedEvent requestedEvent = eventFactory.generateEvent(customer);

        assertThat(requestedEvent.occurredAt()).isEqualTo(Instant.parse("2026-08-28T10:15:30Z"));
    }

    @Test
    void generatesDistinctEventAndCorrelationIds() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-28T10:15:30Z"), ZoneOffset.UTC);

        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID correlationId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Deque<UUID> ids = new ArrayDeque<>(List.of(eventId, correlationId));
        UuidGenerator uuidGenerator = ids::removeFirst;

        CustomerSyncEventFactory eventFactory = new CustomerSyncEventFactory(clock, uuidGenerator);

        Customer customer = new Customer("customer-id", "business-id", "customer name", "Memphis");
        CustomerSyncRequestedEvent requestedEvent = eventFactory.generateEvent(customer);

        assertThat(requestedEvent.eventId()).isEqualTo(eventId);
        assertThat(requestedEvent.correlationId()).isEqualTo(correlationId);
        assertThat(requestedEvent.eventId()).isNotEqualTo(requestedEvent.correlationId());
    }

    @Test
    void serializesCustomerSyncEventContract() throws JacksonException {
        ObjectMapper objectMapper = JsonMapper.builder().build();

        Clock clock = Clock.fixed(Instant.parse("2026-08-28T10:15:30Z"), ZoneOffset.UTC);

        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID correlationId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Deque<UUID> ids = new ArrayDeque<>(List.of(eventId, correlationId));
        UuidGenerator uuidGenerator = ids::removeFirst;

        CustomerSyncEventFactory eventFactory = new CustomerSyncEventFactory(clock, uuidGenerator);

        Customer customer = new Customer("customer-id", "business-id", "customer name", "Memphis");
        CustomerSyncRequestedEvent requestedEvent = eventFactory.generateEvent(customer);

        String json = objectMapper.writeValueAsString(requestedEvent);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("eventId").asString()).isEqualTo(eventId.toString());
        assertThat(root.get("correlationId").asString()).isEqualTo(correlationId.toString());
        assertThat(root.get("occurredAt").asString()).isEqualTo("2026-08-28T10:15:30Z");
        assertThat(root.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(root.get("eventType").asString()).isEqualTo("CUSTOMER_SYNC_REQUESTED");
        assertThat(root.get("sourceSystem").asString()).isEqualTo("SALESFORCE");

        JsonNode customerNode = root.get("customer");
        assertThat(customerNode.get("sourceCustomerId").asString()).isEqualTo("customer-id");
        assertThat(customerNode.get("businessId").asString()).isEqualTo("business-id");
        assertThat(customerNode.get("name").asString()).isEqualTo("customer name");
        assertThat(customerNode.get("billingCity").asString()).isEqualTo("Memphis");
    }
}
