package com.northstar.mockerp.application.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.northstar.mockerp.domain.customer.ErpCustomer;
import com.northstar.mockerp.messaging.customer.CustomerSyncPayload;

class CustomerSyncPayloadToErpCustomerMapperTest {
    private final CustomerSyncPayloadToErpCustomerMapper mapper = new CustomerSyncPayloadToErpCustomerMapper();

    @Test
    void mapsCustomerSyncPayloadToErpCustomer() {
        ErpCustomer customer = mapper.map(new CustomerSyncPayload(" 001ABC123456789 ",
                " northstar-001 ", " Designated Test Account ", " Helsinki "));

        assertThat(customer.sourceCustomerId()).isEqualTo("001ABC123456789");
        assertThat(customer.businessId()).isEqualTo("northstar-001");
        assertThat(customer.name()).isEqualTo("Designated Test Account");
        assertThat(customer.billingCity()).isEqualTo("Helsinki");
    }

    @Test
    void normalizesCustomerSyncPayload() {
        ErpCustomer customer = mapper.map(
                new CustomerSyncPayload(" customer-id ", " business-id ", " Customer Name ", " "));

        assertThat(customer.sourceCustomerId()).isEqualTo("customer-id");
        assertThat(customer.businessId()).isEqualTo("business-id");
        assertThat(customer.name()).isEqualTo("Customer Name");
        assertThat(customer.billingCity()).isNull();
    }
}
