package com.northstar.integrationservice.messaging.customer;

import java.time.Instant;
import java.util.UUID;

public record CustomerSyncRequestedEvent(UUID eventId, UUID correlationId, Instant occurredAt,
        int eventVersion, String eventType, String sourceSystem, CustomerSyncPayload customer) {
}
