package com.northstar.mockerp.application.customer;

import org.springframework.stereotype.Component;

import com.northstar.mockerp.domain.customer.ErpCustomer;
import com.northstar.mockerp.persistence.customer.ErpCustomerEntity;

@Component
public class ErpCustomerToEntityMapper {
    public ErpCustomerEntity map(ErpCustomer erpCustomer) {
        return new ErpCustomerEntity(erpCustomer.sourceCustomerId(), erpCustomer.businessId(),
                erpCustomer.name(), erpCustomer.billingCity());
    }
}
