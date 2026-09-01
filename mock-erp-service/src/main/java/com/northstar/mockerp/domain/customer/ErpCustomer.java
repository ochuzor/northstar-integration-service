package com.northstar.mockerp.domain.customer;

import jakarta.validation.constraints.NotBlank;

public record ErpCustomer(@NotBlank String sourceCustomerId, @NotBlank String businessId,
        @NotBlank String name, String billingCity) {
}
