package com.northstar.integrationservice.web.audit;

import java.time.Instant;
import java.util.UUID;

import com.northstar.integrationservice.domain.audit.CustomerSyncAuditFailureCategory;
import com.northstar.integrationservice.domain.audit.CustomerSyncAuditStatus;

public record CustomerSyncAuditResponse(UUID correlationId, UUID eventId,
        String salesforceAccountId, CustomerSyncAuditStatus status, Instant createdAt,
        Instant updatedAt, CustomerSyncAuditFailureCategory failureCategory) {
}
