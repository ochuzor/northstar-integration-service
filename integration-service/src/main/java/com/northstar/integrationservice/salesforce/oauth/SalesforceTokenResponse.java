package com.northstar.integrationservice.salesforce.oauth;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
final class SalesforceTokenResponse {

    private final String accessToken;
    private final URI instanceUrl;
    private final String tokenType;
    private final String issuedAt;

    @JsonCreator
    SalesforceTokenResponse(@JsonProperty("access_token") String accessToken,
            @JsonProperty("instance_url") URI instanceUrl,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("issued_at") String issuedAt) {
        this.accessToken = accessToken;
        this.instanceUrl = instanceUrl;
        this.tokenType = tokenType;
        this.issuedAt = issuedAt;
    }

    String getAccessToken() {
        return accessToken;
    }

    URI getInstanceUrl() {
        return instanceUrl;
    }

    String getTokenType() {
        return tokenType;
    }

    String getIssuedAt() {
        return issuedAt;
    }
}
