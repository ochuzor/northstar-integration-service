package com.northstar.mockerp.persistence.customer;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ErpProcessedCustomerSyncEventEntityRepository
        extends
            JpaRepository<ErpProcessedCustomerSyncEventEntity, UUID> {
}
