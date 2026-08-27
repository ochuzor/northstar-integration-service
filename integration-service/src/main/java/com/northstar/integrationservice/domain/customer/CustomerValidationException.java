package com.northstar.integrationservice.domain.customer;

import java.util.Set;

public class CustomerValidationException extends RuntimeException {
    private final Set<String> invalidFields;

    public CustomerValidationException(Set<String> invalidFields) {
        super("Customer validation failed");
        this.invalidFields = Set.copyOf(invalidFields);
    }

    public Set<String> getInvalidFields() {
        return this.invalidFields;
    }
}
