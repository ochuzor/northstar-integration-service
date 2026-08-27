package com.northstar.integrationservice.application.customer;

import org.springframework.stereotype.Component;

import com.northstar.integrationservice.application.account.SalesforceAccountResult;
import com.northstar.integrationservice.domain.customer.Customer;

@Component
public class SalesforceAccountToCustomerMapper {

    public Customer map(SalesforceAccountResult source) {
        return new Customer(getTrimmedValueOrNull(source.salesforceAccountId()),
                getTrimmedValueOrNull(source.businessId()), getTrimmedValueOrNull(source.name()),
                getTrimmedValueOrNull(source.billingCity()));
    }

    private String getTrimmedValueOrNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.strip();

        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
