package com.northstar.integrationservice.web.account;

import java.util.UUID;

public record AccountSyncAcceptedResponse(String salesforceAccountId, UUID eventId,
        UUID correlationId, String status) {
}
