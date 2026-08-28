package com.northstar.integrationservice.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.northstar.integrationservice.messaging.customer.CustomerSyncEventFactory;
import com.northstar.integrationservice.messaging.customer.UuidGenerator;

class MessagingConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MessagingConfiguration.class);

    @Test
    void providesCustomerSyncEventFactoryDependencies() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context).hasSingleBean(UuidGenerator.class);
            assertThat(context).hasSingleBean(CustomerSyncEventFactory.class);
        });
    }
}
