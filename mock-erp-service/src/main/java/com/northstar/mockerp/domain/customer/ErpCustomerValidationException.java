package com.northstar.mockerp.domain.customer;

import java.util.Set;

public class ErpCustomerValidationException extends RuntimeException {
    private final Set<String> invalidFields;

    public ErpCustomerValidationException(Set<String> invalidFields) {
        super("ERP customer validation failed");
        this.invalidFields = Set.copyOf(invalidFields);
    }

    public Set<String> getInvalidFields() {
        return invalidFields;
    }
}
