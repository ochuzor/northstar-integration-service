package com.northstar.integrationservice.salesforce.oauth;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.northstar.integrationservice.salesforce.config.SalesforceOAuthProperties;

@Component
public class SalesforceOAuthClient {
    private final RestClient restClient;
    private final SalesforceOAuthProperties properties;

    public SalesforceOAuthClient(RestClient.Builder restClientBuilder,
            SalesforceOAuthProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    SalesforceAuthenticationResult toAuthenticationResult(SalesforceTokenResponse response) {
        if (response == null || !StringUtils.hasText(response.getAccessToken())
                || response.getInstanceUrl() == null
                || !StringUtils.hasText(response.getTokenType())
                || !StringUtils.hasText(response.getIssuedAt())) {
            throw new SalesforceAuthenticationException(
                    "Salesforce returned an invalid authentication response",
                    HttpStatus.OK.value());
        }

        long issuedAtMilliseconds;
        try {

            String issuedAtText = response.getIssuedAt();
            issuedAtMilliseconds = Long.parseLong(issuedAtText);
        } catch (NumberFormatException exception) {
            throw new SalesforceAuthenticationException(
                    "Salesforce returned an invalid authentication response",
                    HttpStatus.OK.value());
        }

        Instant issuedAt = Instant.ofEpochMilli(issuedAtMilliseconds);

        return new SalesforceAuthenticationResult(true, response.getTokenType(), issuedAt);
    }

    public SalesforceAuthenticationResult authenticate() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());

        try {
            SalesforceTokenResponse result = restClient.post().uri(properties.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve()
                    .onStatus(status -> status.is4xxClientError(), (request, response) -> {
                        HttpStatusCode status = response.getStatusCode();

                        throw new SalesforceAuthenticationException(
                                "Salesforce rejected the authentication request", status.value());
                    }).onStatus(status -> status.is5xxServerError(), (request, response) -> {
                        HttpStatusCode status = response.getStatusCode();

                        throw new SalesforceAuthenticationException(
                                "Salesforce authentication server failed", status.value());
                    }).body(SalesforceTokenResponse.class);

            return toAuthenticationResult(result);
        } catch (ResourceAccessException exception) {
            throw new SalesforceAuthenticationUnavailableException();
        } catch (RestClientException exception) {
            if (exception.getCause() instanceof HttpMessageNotReadableException) {
                throw new SalesforceAuthenticationException(
                        "Salesforce returned an invalid authentication response",
                        HttpStatus.OK.value());
            }

            throw exception;
        }
    }
}
