package com.northstar.integrationservice.application.customer;

import org.springframework.stereotype.Service;

import com.northstar.integrationservice.application.account.SalesforceAccountResult;
import com.northstar.integrationservice.application.account.SalesforceAccountService;
import com.northstar.integrationservice.domain.customer.Customer;

@Service
public class CustomerPreparationService {
    private final SalesforceAccountService accountService;
    private final SalesforceAccountToCustomerMapper mapper;
    private final CustomerValidator validator;

    public CustomerPreparationService(SalesforceAccountService accountService,
            SalesforceAccountToCustomerMapper mapper, CustomerValidator validator) {
        this.accountService = accountService;
        this.mapper = mapper;
        this.validator = validator;
    }

    public Customer prepareCustomer(String salesforceAccountId) {
        SalesforceAccountResult result = this.accountService.fetchAccount(salesforceAccountId);
        Customer customer = this.mapper.map(result);

        return this.validator.validate(customer);
    }
}
