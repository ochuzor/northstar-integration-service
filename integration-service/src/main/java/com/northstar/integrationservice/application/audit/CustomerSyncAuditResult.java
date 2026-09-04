package com.northstar.integrationservice.application.audit;

import java.time.Instant;
import java.util.UUID;

import com.northstar.integrationservice.domain.audit.CustomerSyncAuditFailureCategory;
import com.northstar.integrationservice.domain.audit.CustomerSyncAuditStatus;

public record CustomerSyncAuditResult(UUID correlationId, UUID eventId, String salesforceAccountId,
        CustomerSyncAuditStatus status, Instant createdAt, Instant updatedAt,
        CustomerSyncAuditFailureCategory failureCategory) {
}
