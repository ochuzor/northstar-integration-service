package com.northstar.integrationservice.salesforce.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "salesforce.oauth")
@Validated
public final class SalesforceOAuthProperties {

    @NotNull
    private final URI tokenUrl;

    @NotBlank
    private final String clientId;

    @NotBlank
    private final String clientSecret;

    public SalesforceOAuthProperties(URI tokenUrl, String clientId, String clientSecret) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public URI getTokenUrl() {
        return this.tokenUrl;
    }

    public String getClientId() {
        return this.clientId;
    }

    public String getClientSecret() {
        return this.clientSecret;
    }
}
