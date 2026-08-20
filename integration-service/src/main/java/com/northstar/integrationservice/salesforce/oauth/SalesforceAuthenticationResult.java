package com.northstar.integrationservice.salesforce.oauth;

import java.time.Instant;

public record SalesforceAuthenticationResult(boolean authenticated, String tokenType,
        Instant issuedAt) {
}
