package com.northstar.integrationservice.web.audit;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.northstar.integrationservice.application.audit.CustomerSyncAuditResult;
import com.northstar.integrationservice.application.audit.CustomerSyncAuditService;

@RestController
@RequestMapping("/api/sync")
public class CustomerSyncAuditController {
    private final CustomerSyncAuditService syncService;

    public CustomerSyncAuditController(CustomerSyncAuditService syncService) {
        this.syncService = syncService;
    }

    @GetMapping("/{correlationId}")
    public ResponseEntity<CustomerSyncAuditResponse> getAudit(@PathVariable UUID correlationId) {
        CustomerSyncAuditResult auditResult = syncService.findByCorrelationId(correlationId);

        CustomerSyncAuditResponse response = new CustomerSyncAuditResponse(
                auditResult.correlationId(), auditResult.eventId(),
                auditResult.salesforceAccountId(), auditResult.status(), auditResult.createdAt(),
                auditResult.updatedAt(), auditResult.failureCategory());

        return ResponseEntity.ok().body(response);
    }
}
