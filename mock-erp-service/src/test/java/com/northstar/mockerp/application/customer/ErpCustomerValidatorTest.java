package com.northstar.mockerp.application.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.northstar.mockerp.domain.customer.ErpCustomer;
import com.northstar.mockerp.domain.customer.ErpCustomerValidationException;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ErpCustomerValidatorTest {
    private final Validator jakartaValidator = Validation.buildDefaultValidatorFactory()
            .getValidator();
    private final ErpCustomerValidator validator = new ErpCustomerValidator(jakartaValidator);

    @Test
    void acceptsValidErpCustomer() {
        ErpCustomer customer = new ErpCustomer("customer-id", "business-id", "Customer Name", null);

        ErpCustomer validatedCustomer = validator.validate(customer);

        assertThat(validatedCustomer).isSameAs(customer);
    }

    @Test
    void rejectsErpCustomerMissingRequiredFields() {
        ErpCustomer customer = new ErpCustomer(null, " ", null, null);

        assertThatThrownBy(() -> validator.validate(customer))
                .isInstanceOfSatisfying(ErpCustomerValidationException.class, exception -> {
                    assertThat(exception.getInvalidFields())
                            .containsExactlyInAnyOrder("sourceCustomerId", "businessId", "name");
                    assertThat(exception).hasMessage("ERP customer validation failed");
                });
    }
}
