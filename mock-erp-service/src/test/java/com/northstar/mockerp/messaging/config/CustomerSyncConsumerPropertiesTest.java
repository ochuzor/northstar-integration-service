package com.northstar.mockerp.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class CustomerSyncConsumerPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues("northstar.messaging.customer-sync.max-attempts=3",
                    "northstar.messaging.customer-sync.retry-backoff=1s",
                    "northstar.messaging.customer-sync.dead-letter-suffix=.DLT");

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CustomerSyncConsumerProperties.class)
    static class TestConfiguration {
    }

    @Test
    void bindsRetryAndDeadLetterProperties() {
        contextRunner.withPropertyValues(
                "northstar.messaging.customer-sync.topic=northstar.customer-sync.v1",
                "northstar.messaging.customer-sync.group-id=northstar.mock-erp.v1",
                "northstar.messaging.customer-sync.listener-enabled=false").run(context -> {
                    assertThat(context).hasNotFailed();

                    CustomerSyncConsumerProperties properties = context
                            .getBean(CustomerSyncConsumerProperties.class);

                    assertThat(properties.topic()).isEqualTo("northstar.customer-sync.v1");
                    assertThat(properties.groupId()).isEqualTo("northstar.mock-erp.v1");
                    assertThat(properties.listenerEnabled()).isFalse();
                    assertThat(properties.maxAttempts()).isEqualTo(3);
                    assertThat(properties.retryBackoff())
                            .isEqualTo(java.time.Duration.ofSeconds(1));
                    assertThat(properties.deadLetterSuffix()).isEqualTo(".DLT");
                });
    }

    @Test
    void rejectsBlankConsumerGroup() {
        contextRunner.withPropertyValues(
                "northstar.messaging.customer-sync.topic=northstar.customer-sync.v1",
                "northstar.messaging.customer-sync.group-id=",
                "northstar.messaging.customer-sync.listener-enabled=false").run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class).rootCause()
                            .hasMessageContaining("groupId")
                            .hasMessageContaining("must not be blank");
                });
    }

    @Test
    void rejectsMaxAttemptsBelowOne() {
        contextRunner.withPropertyValues("northstar.messaging.customer-sync.topic=topic",
                "northstar.messaging.customer-sync.group-id=group",
                "northstar.messaging.customer-sync.max-attempts=0").run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class).rootCause()
                            .hasMessageContaining("maxAttempts")
                            .hasMessageContaining("must be greater than or equal to 1");
                });
    }

    @Test
    void rejectsNonPositiveRetryBackoff() {
        contextRunner.withPropertyValues("northstar.messaging.customer-sync.topic=topic",
                "northstar.messaging.customer-sync.group-id=group",
                "northstar.messaging.customer-sync.retry-backoff=0s").run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class).rootCause()
                            .hasMessageContaining("retryBackoffPositive")
                            .hasMessageContaining("retryBackoff must be greater than zero");
                });
    }
}
