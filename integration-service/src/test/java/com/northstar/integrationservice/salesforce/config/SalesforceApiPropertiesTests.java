package com.northstar.integrationservice.salesforce.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class SalesforceApiPropertiesTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsSalesforceApiVersion() {
        contextRunner.withPropertyValues("salesforce.api.version=v66.0").run(context -> {
            assertThat(context).hasNotFailed();

            SalesforceApiProperties properties = context.getBean(SalesforceApiProperties.class);

            assertThat(properties.version()).isEqualTo("v66.0");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SalesforceApiProperties.class)
    static class TestConfiguration {
    }

    @Test
    void rejectsBlankSalesforceApiVersion() {
        contextRunner.withPropertyValues("salesforce.api.version=").run(context -> {
            assertThat(context).hasFailed();

            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(BindValidationException.class).rootCause()
                    .hasMessageContaining("version").hasMessageContaining("must not be blank");
        });
    }
}
