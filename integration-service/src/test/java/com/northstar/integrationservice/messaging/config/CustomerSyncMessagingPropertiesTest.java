package com.northstar.integrationservice.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class CustomerSyncMessagingPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CustomerSyncMessagingProperties.class)
    static class TestConfiguration {
    }

    @Test
    void bindsCustomerSyncConfiguration() {
        contextRunner
                .withPropertyValues(
                        "northstar.messaging.customer-sync.topic=northstar.customer-sync.v1",
                        "northstar.messaging.customer-sync.acknowledgement-timeout=5s")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    CustomerSyncMessagingProperties properties = context
                            .getBean(CustomerSyncMessagingProperties.class);

                    assertThat(properties.topic()).isEqualTo("northstar.customer-sync.v1");
                    assertThat(properties.acknowledgementTimeout())
                            .isEqualTo(Duration.ofSeconds(5));
                });
    }

    @Test
    void rejectsBlankCustomerSyncTopic() {
        contextRunner
                .withPropertyValues("northstar.messaging.customer-sync.topic=",
                        "northstar.messaging.customer-sync.acknowledgement-timeout=5s")
                .run(context -> {
                    assertThat(context).hasFailed();

                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class).rootCause()
                            .hasMessageContaining("topic")
                            .hasMessageContaining("must not be blank");
                });
    }

    @Test
    void rejectsNonPositiveAcknowledgementTimeout() {
        contextRunner
                .withPropertyValues(
                        "northstar.messaging.customer-sync.topic=northstar.customer-sync.v1",
                        "northstar.messaging.customer-sync.acknowledgement-timeout=0ms")
                .run(context -> {
                    assertThat(context).hasFailed();

                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class).rootCause()
                            .hasMessageContaining("acknowledgementTimeout");
                });
    }
}
