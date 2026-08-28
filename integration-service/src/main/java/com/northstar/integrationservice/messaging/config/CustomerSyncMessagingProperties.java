package com.northstar.integrationservice.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties("northstar.messaging.customer-sync")
@Validated
public record CustomerSyncMessagingProperties(@NotBlank String topic) {
}
