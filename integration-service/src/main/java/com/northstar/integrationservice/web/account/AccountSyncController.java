package com.northstar.integrationservice.web.account;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.northstar.integrationservice.application.customer.CustomerPreparationService;
import com.northstar.integrationservice.domain.customer.Customer;

@RestController
@RequestMapping("/api/sync/account")
public class AccountSyncController {
    private final CustomerPreparationService customerPreparationService;

    public AccountSyncController(CustomerPreparationService customerPreparationService) {
        this.customerPreparationService = customerPreparationService;
    }

    @PostMapping("/{salesforceAccountId}")
    public AccountSyncResponse syncAccount(@PathVariable String salesforceAccountId) {
        Customer customer = this.customerPreparationService.prepareCustomer(salesforceAccountId);

        return new AccountSyncResponse(customer.sourceCustomerId(), customer.name(),
                customer.businessId(), customer.billingCity());
    }
}
