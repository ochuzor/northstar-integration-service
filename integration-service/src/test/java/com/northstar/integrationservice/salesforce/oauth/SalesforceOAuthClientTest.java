package com.northstar.integrationservice.salesforce.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class SalesforceOAuthClientTest {
    @Test
    void createsAuthenticationResult() {
        SalesforceOAuthClient client = new SalesforceOAuthClient();
        SalesforceTokenResponse tokenResponse = new SalesforceTokenResponse("token",
                URI.create("http://example.com"), "Bearer", "1784563200000");

        SalesforceAuthenticationResult result = client.toAuthenticationResult(tokenResponse);

        assertThat(result.authenticated()).isTrue();
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.issuedAt()).isEqualTo(Instant.ofEpochMilli(1_784_563_200_000L));
    }
}
