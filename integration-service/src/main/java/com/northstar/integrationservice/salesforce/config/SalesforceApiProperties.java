package com.northstar.integrationservice.salesforce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "salesforce.api")
@Validated
public record SalesforceApiProperties(@NotBlank String version) {
}
