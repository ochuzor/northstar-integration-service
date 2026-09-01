package com.northstar.mockerp.application.customer;

import org.springframework.stereotype.Component;

import com.northstar.mockerp.domain.customer.ErpCustomer;
import com.northstar.mockerp.messaging.customer.CustomerSyncPayload;

@Component
public class CustomerSyncPayloadToErpCustomerMapper {
    public ErpCustomer map(CustomerSyncPayload payload) {
        return new ErpCustomer(getTrimmedValueOrNull(payload.sourceCustomerId()),
                getTrimmedValueOrNull(payload.businessId()), getTrimmedValueOrNull(payload.name()),
                getTrimmedValueOrNull(payload.billingCity()));
    }

    private String getTrimmedValueOrNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.strip();

        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
