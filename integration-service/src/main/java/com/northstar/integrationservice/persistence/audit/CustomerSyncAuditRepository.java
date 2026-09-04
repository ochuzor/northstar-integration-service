package com.northstar.integrationservice.persistence.audit;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerSyncAuditRepository extends JpaRepository<CustomerSyncAuditEntity, UUID> {

    Optional<CustomerSyncAuditEntity> findByEventId(UUID eventId);
}
