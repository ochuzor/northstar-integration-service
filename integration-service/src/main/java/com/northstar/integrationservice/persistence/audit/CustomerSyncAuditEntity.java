package com.northstar.integrationservice.persistence.audit;

import java.time.Instant;
import java.util.UUID;

import com.northstar.integrationservice.domain.audit.CustomerSyncAuditStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_sync_audits")
public class CustomerSyncAuditEntity {

    @Id
    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "event_id", unique = true)
    private UUID eventId;

    @Column(name = "source_customer_id", nullable = false, length = 18)
    private String sourceCustomerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CustomerSyncAuditStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "failure_category", length = 64)
    private String failureCategory;

    protected CustomerSyncAuditEntity() {
    }

    public CustomerSyncAuditEntity(UUID correlationId, UUID eventId, String sourceCustomerId,
            CustomerSyncAuditStatus status, Instant createdAt, Instant updatedAt,
            String failureCategory) {
        this.correlationId = correlationId;
        this.eventId = eventId;
        this.sourceCustomerId = sourceCustomerId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.failureCategory = failureCategory;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getSourceCustomerId() {
        return sourceCustomerId;
    }

    public CustomerSyncAuditStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getFailureCategory() {
        return failureCategory;
    }
}
