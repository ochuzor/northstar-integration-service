package com.northstar.integrationservice.salesforce;

import java.net.URI;
import java.time.Instant;

public final class SalesforceSession {
    private final String accessToken;
    private final URI instanceUrl;
    private final String tokenType;
    private final Instant issuedAt;

    public SalesforceSession(String accessToken, URI instanceUrl, String tokenType,
            Instant issuedAt) {
        this.accessToken = accessToken;
        this.instanceUrl = instanceUrl;
        this.tokenType = tokenType;
        this.issuedAt = issuedAt;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public URI getInstanceUrl() {
        return this.instanceUrl;
    }

    public String getTokenType() {
        return this.tokenType;
    }

    public Instant getIssuedAt() {
        return this.issuedAt;
    }
}
