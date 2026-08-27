package com.northstar.integrationservice.application.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.northstar.integrationservice.domain.customer.Customer;
import com.northstar.integrationservice.domain.customer.CustomerValidationException;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CustomerValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private final CustomerValidator customerValidator = new CustomerValidator(validator);

    @Test
    void acceptsValidCustomer() {
        Customer customer = new Customer("test-id", "test-business-id", "test business name",
                "New York");

        Customer validatedCustomer = customerValidator.validate(customer);

        assertThat(validatedCustomer).isSameAs(customer);
    }

    @Test
    void rejectsCustomerWithoutSourceCustomerId() {
        Customer customer = new Customer(null, "test-business-id", "test business name",
                "New York");

        assertThatThrownBy(() -> customerValidator.validate(customer))
                .isInstanceOfSatisfying(CustomerValidationException.class, exception -> {
                    assertThat(exception.getInvalidFields()).containsExactly("sourceCustomerId");
                    assertThat(exception).hasMessage("Customer validation failed");
                });
    }

    @Test
    void rejectsCustomerWithoutBusinessId() {
        Customer customer = new Customer("test-id", "  ", "test business name", "New York");

        assertThatThrownBy(() -> customerValidator.validate(customer))
                .isInstanceOfSatisfying(CustomerValidationException.class, exception -> {
                    assertThat(exception.getInvalidFields()).containsExactly("businessId");
                    assertThat(exception).hasMessage("Customer validation failed");
                });
    }

    @Test
    void rejectsCustomerWithoutName() {
        Customer customer = new Customer("test-id", "Biz Id", " ", "New York");

        assertThatThrownBy(() -> customerValidator.validate(customer))
                .isInstanceOfSatisfying(CustomerValidationException.class, exception -> {
                    assertThat(exception.getInvalidFields()).containsExactly("name");
                    assertThat(exception).hasMessage("Customer validation failed");
                });
    }

    @Test
    void allowsCustomerWithoutBillingCity() {
        Customer customer = new Customer("test-id", "Biz Id", "Biz Name", null);

        Customer validatedCustomer = customerValidator.validate(customer);

        assertThat(validatedCustomer).isSameAs(customer);
    }
}
