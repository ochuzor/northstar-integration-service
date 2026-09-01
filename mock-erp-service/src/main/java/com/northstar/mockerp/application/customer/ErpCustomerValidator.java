package com.northstar.mockerp.application.customer;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.northstar.mockerp.domain.customer.ErpCustomer;
import com.northstar.mockerp.domain.customer.ErpCustomerValidationException;

import jakarta.validation.Validator;

@Component
public class ErpCustomerValidator {
    private final Validator validator;

    public ErpCustomerValidator(Validator validator) {
        this.validator = validator;
    }

    public ErpCustomer validate(ErpCustomer customer) {
        Set<String> invalidFields = validator.validate(customer).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toUnmodifiableSet());

        if (!invalidFields.isEmpty()) {
            throw new ErpCustomerValidationException(invalidFields);
        }

        return customer;
    }
}
