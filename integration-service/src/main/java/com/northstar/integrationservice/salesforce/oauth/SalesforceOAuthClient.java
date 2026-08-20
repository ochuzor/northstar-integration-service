package com.northstar.integrationservice.salesforce.oauth;

import java.time.Instant;

public class SalesforceOAuthClient {
    SalesforceAuthenticationResult toAuthenticationResult(SalesforceTokenResponse response) {

        String issuedAtText = response.getIssuedAt();
        long issuedAtMilliseconds = Long.parseLong(issuedAtText);
        Instant issuedAt = Instant.ofEpochMilli(issuedAtMilliseconds);

        return new SalesforceAuthenticationResult(true, response.getTokenType(), issuedAt);
    }
}
