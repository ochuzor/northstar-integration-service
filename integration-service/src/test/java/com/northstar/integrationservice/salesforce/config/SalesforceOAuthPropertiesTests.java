package com.northstar.integrationservice.salesforce.config;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class SalesforceOAuthPropertiesTests {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsValidProperties() {
        contextRunner
                .withPropertyValues(
                        "salesforce.oauth.token-url=https://auth.example.test/services/oauth2/token",
                        "salesforce.oauth.client-id=test-client",
                        "salesforce.oauth.client-secret=test-secret")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    SalesforceOAuthProperties properties =
                            context.getBean(SalesforceOAuthProperties.class);

                    assertThat(properties.getTokenUrl())
                            .isEqualTo(URI.create(
                                    "https://auth.example.test/services/oauth2/token"));
                    assertThat(properties.getClientId()).isEqualTo("test-client");
                    assertThat(properties.getClientSecret()).isEqualTo("test-secret");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SalesforceOAuthProperties.class)
    static class TestConfiguration {
    }

    @Test
    void rejectsBlankClientSecret() {
        contextRunner
                .withPropertyValues(
                        "salesforce.oauth.token-url=https://auth.example.test/services/oauth2/token",
                        "salesforce.oauth.client-id=test-client",
                        "salesforce.oauth.client-secret=")
                .run(context -> {
                    assertThat(context).hasFailed();

                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .rootCause()
                            .hasMessageContaining("clientSecret")
                            .hasMessageContaining("must not be blank");
                });
    }

    @Test
    void rejectsBlankTokenUrl() {
        contextRunner
                .withPropertyValues(
                        "salesforce.oauth.token-url=",
                        "salesforce.oauth.client-id=test-client",
                        "salesforce.oauth.client-secret=test-secret")
                .run(context -> {
                    assertThat(context).hasFailed();

                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .rootCause()
                            .hasMessageContaining("tokenUrl")
                            .hasMessageContaining("must not be null");
                });
    }

    @Test
    void rejectsBlankClientId() {
        contextRunner
                .withPropertyValues(
                        "salesforce.oauth.token-url=https://auth.example.test/services/oauth2/token",
                        "salesforce.oauth.client-id=",
                        "salesforce.oauth.client-secret=test-secret")
                .run(context -> {
                    assertThat(context).hasFailed();

                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .rootCause()
                            .hasMessageContaining("clientId")
                            .hasMessageContaining("must not be blank");
                });
    }

    @Test
    void rejectsMalformedTokenUrl() {
        contextRunner
                .withPropertyValues(
                        "salesforce.oauth.token-url=https://",
                        "salesforce.oauth.client-id=test-client",
                        "salesforce.oauth.client-secret=test-secret")
                .run(context -> {
                    assertThat(context).hasFailed();

                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalArgumentException.class);
                });
    }
}
