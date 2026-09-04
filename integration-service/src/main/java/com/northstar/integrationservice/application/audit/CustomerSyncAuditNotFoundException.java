package com.northstar.integrationservice.application.audit;

import java.util.UUID;

public class CustomerSyncAuditNotFoundException extends RuntimeException {
    public CustomerSyncAuditNotFoundException(UUID correlationId) {
        super("Audit not found: " + correlationId);
    }
}
