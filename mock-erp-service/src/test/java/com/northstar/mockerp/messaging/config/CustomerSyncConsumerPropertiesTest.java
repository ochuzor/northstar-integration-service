package com.northstar.mockerp.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class CustomerSyncConsumerPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CustomerSyncConsumerProperties.class)
    static class TestConfiguration {
    }

    @Test
    void bindsCustomerSyncConsumerProperties() {
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
}
