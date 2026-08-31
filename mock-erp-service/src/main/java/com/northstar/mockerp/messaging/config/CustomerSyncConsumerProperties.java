package com.northstar.mockerp.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties("northstar.messaging.customer-sync")
@Validated
public record CustomerSyncConsumerProperties(@NotBlank String topic, @NotBlank String groupId,
        boolean listenerEnabled) {
}
