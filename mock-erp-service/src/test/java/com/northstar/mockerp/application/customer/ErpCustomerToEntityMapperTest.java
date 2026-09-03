package com.northstar.mockerp.application.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.northstar.mockerp.domain.customer.ErpCustomer;
import com.northstar.mockerp.persistence.customer.ErpCustomerEntity;

class ErpCustomerToEntityMapperTest {

    @Test
    void mapsErpCustomerToEntity() {
        ErpCustomerToEntityMapper mapper = new ErpCustomerToEntityMapper();
        ErpCustomer customer = new ErpCustomer("001ABC123456789012", "NORTHSTAR-001",
                "Designated Test Account", "Helsinki");

        ErpCustomerEntity entity = mapper.map(customer);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getSourceCustomerId()).isEqualTo("001ABC123456789012");
        assertThat(entity.getBusinessId()).isEqualTo("NORTHSTAR-001");
        assertThat(entity.getName()).isEqualTo("Designated Test Account");
        assertThat(entity.getBillingCity()).isEqualTo("Helsinki");
    }
}
