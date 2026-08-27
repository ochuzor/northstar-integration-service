package com.northstar.integrationservice.application.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.northstar.integrationservice.application.account.SalesforceAccountResult;
import com.northstar.integrationservice.domain.customer.Customer;

class SalesforceAccountToCustomerMapperTest {
    private final SalesforceAccountToCustomerMapper mapper = new SalesforceAccountToCustomerMapper();

    @Test
    void mapsSalesforceAccountToCustomer() {
        Customer customer = mapper.map(new SalesforceAccountResult(" 001ABC123456789 ",
                " Designated Test Account ", " northstar-001 ", " Helsinki "));

        assertThat(customer.sourceCustomerId()).isEqualTo("001ABC123456789");
        assertThat(customer.businessId()).isEqualTo("northstar-001");
        assertThat(customer.name()).isEqualTo("Designated Test Account");
        assertThat(customer.billingCity()).isEqualTo("Helsinki");
    }

    @Test
    void mapsBlankBillingCityToNull() {
        Customer customer = mapper.map(new SalesforceAccountResult(" 001ABC123456789 ",
                " Designated Test Account ", " northstar-001 ", "   "));

        assertThat(customer.sourceCustomerId()).isEqualTo("001ABC123456789");
        assertThat(customer.businessId()).isEqualTo("northstar-001");
        assertThat(customer.name()).isEqualTo("Designated Test Account");
        assertThat(customer.billingCity()).isNull();
    }
}
