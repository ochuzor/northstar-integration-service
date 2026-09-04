package com.northstar.integrationservice.persistence.audit;

import java.time.Instant;
import java.util.UUID;

import com.northstar.integrationservice.domain.audit.CustomerSyncAuditFailureCategory;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_category", length = 64)
    private CustomerSyncAuditFailureCategory failureCategory;

    protected CustomerSyncAuditEntity() {
    }

    public CustomerSyncAuditEntity(UUID correlationId, UUID eventId, String sourceCustomerId,
            CustomerSyncAuditStatus status, Instant createdAt, Instant updatedAt,
            CustomerSyncAuditFailureCategory failureCategory) {
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

    public CustomerSyncAuditFailureCategory getFailureCategory() {
        return failureCategory;
    }

    public void markPublished(Instant transitionTime) {
        this.status = CustomerSyncAuditStatus.PUBLISHED;
        this.updatedAt = transitionTime;
        this.failureCategory = null;
    }

    public void markPublicationFailed(Instant transitionTime) {
        this.status = CustomerSyncAuditStatus.PUBLICATION_FAILED;
        this.updatedAt = transitionTime;
        this.failureCategory = CustomerSyncAuditFailureCategory.KAFKA_PUBLICATION;
    }
}
