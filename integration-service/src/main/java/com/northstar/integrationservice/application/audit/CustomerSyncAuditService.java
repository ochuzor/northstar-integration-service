package com.northstar.integrationservice.application.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.integrationservice.domain.audit.CustomerSyncAuditStatus;
import com.northstar.integrationservice.messaging.customer.CustomerSyncRequestedEvent;
import com.northstar.integrationservice.persistence.audit.CustomerSyncAuditEntity;
import com.northstar.integrationservice.persistence.audit.CustomerSyncAuditRepository;

@Service
public class CustomerSyncAuditService {

    private final CustomerSyncAuditRepository repository;
    private final Clock clock;

    public CustomerSyncAuditService(CustomerSyncAuditRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordInitiated(CustomerSyncRequestedEvent event) {
        Instant initiatedAt = Instant.now(clock);
        repository.save(new CustomerSyncAuditEntity(event.correlationId(), event.eventId(),
                event.customer().sourceCustomerId(), CustomerSyncAuditStatus.INITIATED, initiatedAt,
                initiatedAt, null));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID correlationId) {
        CustomerSyncAuditEntity audit = findAudit(correlationId);
        audit.markPublished(Instant.now(clock));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublicationFailed(UUID correlationId) {
        CustomerSyncAuditEntity audit = findAudit(correlationId);
        audit.markPublicationFailed(Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public CustomerSyncAuditResult findByCorrelationId(UUID correlationId) {
        CustomerSyncAuditEntity audit = repository.findById(correlationId)
                .orElseThrow(() -> new CustomerSyncAuditNotFoundException(correlationId));

        return new CustomerSyncAuditResult(audit.getCorrelationId(), audit.getEventId(),
                audit.getSourceCustomerId(), audit.getStatus(), audit.getCreatedAt(),
                audit.getUpdatedAt(), audit.getFailureCategory());
    }

    private CustomerSyncAuditEntity findAudit(UUID correlationId) {
        return repository.findById(correlationId)
                .orElseThrow(() -> new IllegalStateException("Customer sync audit not found"));
    }
}
