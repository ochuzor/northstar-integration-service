package com.northstar.integrationservice.web.account;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.northstar.integrationservice.application.account.SalesforceAccountResult;
import com.northstar.integrationservice.application.account.SalesforceAccountService;

@RestController
@RequestMapping("/api/sync/account")
public class AccountSyncController {

    private final SalesforceAccountService accountService;

    public AccountSyncController(SalesforceAccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/{salesforceAccountId}")
    public SalesforceAccountResult syncAccount(@PathVariable String salesforceAccountId) {
        return accountService.fetchAccount(salesforceAccountId);
    }
}
