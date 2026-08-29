package com.northstar.integrationservice.messaging.customer;

import java.util.UUID;

public record CustomerSyncPublicationResult(UUID eventId, UUID correlationId,
        String sourceCustomerId) {
}
