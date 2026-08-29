package com.northstar.integrationservice.messaging.config;

import java.time.Duration;

import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties("northstar.messaging.customer-sync")
@Validated
public record CustomerSyncMessagingProperties(@NotBlank String topic,
        @NotNull @DurationMin(millis = 1) Duration acknowledgementTimeout) {
}
