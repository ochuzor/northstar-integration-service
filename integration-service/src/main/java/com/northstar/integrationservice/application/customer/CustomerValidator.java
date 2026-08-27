package com.northstar.integrationservice.application.customer;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.northstar.integrationservice.domain.customer.Customer;
import com.northstar.integrationservice.domain.customer.CustomerValidationException;

import jakarta.validation.Validator;

@Component
public class CustomerValidator {

    private final Validator validator;

    public CustomerValidator(Validator validator) {
        this.validator = validator;
    }

    public Customer validate(Customer customer) {
        Set<String> invalidFields = validator.validate(customer).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toUnmodifiableSet());

        if (!invalidFields.isEmpty()) {
            throw new CustomerValidationException(invalidFields);
        }

        return customer;
    }
}
