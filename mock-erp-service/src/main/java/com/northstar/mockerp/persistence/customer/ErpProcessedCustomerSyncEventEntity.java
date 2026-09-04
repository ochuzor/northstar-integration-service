package com.northstar.mockerp.persistence.customer;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_customer_sync_events")
public class ErpProcessedCustomerSyncEventEntity {
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "source_customer_id", nullable = false, length = 18)
    private String sourceCustomerId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ErpProcessedCustomerSyncEventEntity() {
    }

    public ErpProcessedCustomerSyncEventEntity(UUID eventId, String sourceCustomerId,
            Instant processedAt) {
        this.eventId = eventId;
        this.sourceCustomerId = sourceCustomerId;
        this.processedAt = processedAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getSourceCustomerId() {
        return sourceCustomerId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
