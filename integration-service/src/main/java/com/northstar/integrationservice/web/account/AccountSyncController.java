package com.northstar.integrationservice.web.account;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.northstar.integrationservice.application.customer.CustomerSynchronizationService;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationResult;

@RestController
@RequestMapping("/api/sync/account")
public class AccountSyncController {
    private final CustomerSynchronizationService synchronizationService;

    public AccountSyncController(CustomerSynchronizationService customerSyncService) {
        this.synchronizationService = customerSyncService;
    }

    @PostMapping("/{salesforceAccountId}")
    public ResponseEntity<AccountSyncAcceptedResponse> syncAccount(
            @PathVariable String salesforceAccountId) {
        CustomerSyncPublicationResult result = synchronizationService
                .synchronizeAccount(salesforceAccountId);
        AccountSyncAcceptedResponse response = new AccountSyncAcceptedResponse(
                result.sourceCustomerId(), result.eventId(), result.correlationId(), "ACCEPTED");

        return ResponseEntity.accepted().body(response);
    }
}
