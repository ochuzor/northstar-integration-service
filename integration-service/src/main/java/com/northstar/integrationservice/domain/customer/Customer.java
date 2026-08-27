package com.northstar.integrationservice.domain.customer;

import jakarta.validation.constraints.NotBlank;

public record Customer(@NotBlank String sourceCustomerId, @NotBlank String businessId,
        @NotBlank String name, String billingCity) {
}
