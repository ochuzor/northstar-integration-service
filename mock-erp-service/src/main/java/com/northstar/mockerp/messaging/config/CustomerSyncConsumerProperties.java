package com.northstar.mockerp.messaging.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties("northstar.messaging.customer-sync")
@Validated
public record CustomerSyncConsumerProperties(@NotBlank String topic, @NotBlank String groupId,
        boolean listenerEnabled, @Min(1) int maxAttempts, @NotNull Duration retryBackoff,
        @NotBlank String deadLetterSuffix) {

    @AssertTrue(message = "retryBackoff must be greater than zero")
    public boolean isRetryBackoffPositive() {
        return retryBackoff != null && !retryBackoff.isZero() && !retryBackoff.isNegative();
    }
}
