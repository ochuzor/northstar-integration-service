package com.northstar.integrationservice.salesforce.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Tag("live-salesforce")
@SpringBootTest
class SalesforceOAuthLiveSmokeTest {

    private final SalesforceOAuthClient client;

    @Autowired
    SalesforceOAuthLiveSmokeTest(SalesforceOAuthClient client) {
        this.client = client;
    }

    @Test
    void authenticatesAgainstSalesforce() {
        SalesforceAuthenticationResult result = client.authenticate();

        assertThat(result.authenticated()).isTrue();
        assertThat(result.tokenType()).isEqualToIgnoringCase("Bearer");
        assertThat(result.issuedAt()).isNotNull();
    }
}
