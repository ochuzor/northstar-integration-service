package com.northstar.integrationservice.salesforce.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.northstar.integrationservice.salesforce.config.SalesforceOAuthProperties;

class SalesforceOAuthClientTest {
    @Test
    void createsAuthenticationResult() {
        SalesforceOAuthProperties properties = new SalesforceOAuthProperties(
                URI.create("https://auth.example.test/services/oauth2/token"), "test-client",
                "test-secret");

        SalesforceOAuthClient client = new SalesforceOAuthClient(RestClient.builder(), properties);

        SalesforceTokenResponse tokenResponse = new SalesforceTokenResponse("token",
                URI.create("http://example.com"), "Bearer", "1784563200000");

        SalesforceAuthenticationResult result = client.toAuthenticationResult(tokenResponse);

        assertThat(result.authenticated()).isTrue();
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.issuedAt()).isEqualTo(Instant.ofEpochMilli(1_784_563_200_000L));
    }

    @Test
    void authenticatesUsingClientCredentials() {
        URI tokenUrl = URI.create("https://auth.example.test/services/oauth2/token");

        SalesforceOAuthProperties properties = new SalesforceOAuthProperties(tokenUrl,
                "test-client", "test-secret");

        RestClient.Builder restClientBuilder = RestClient.builder();

        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

        SalesforceOAuthClient client = new SalesforceOAuthClient(restClientBuilder, properties);

        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("grant_type", "client_credentials");
        expectedForm.add("client_id", "test-client");
        expectedForm.add("client_secret", "test-secret");

        server.expect(requestTo(tokenUrl)).andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(expectedForm)).andRespond(withSuccess("""
                        {
                          "access_token": "synthetic-token",
                          "instance_url": "https://instance.example.test",
                          "token_type": "Bearer",
                          "issued_at": "1784563200000",
                          "unexpected_field": "ignored"
                        }
                        """, MediaType.APPLICATION_JSON));

        SalesforceAuthenticationResult result = client.authenticate();

        assertThat(result.authenticated()).isTrue();
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.issuedAt()).isEqualTo(Instant.ofEpochMilli(1_784_563_200_000L));

        server.verify();
    }

    @Test
    void authenticateHandlesBadRequest() {
        URI tokenUrl = URI.create("https://auth.example.test/services/oauth2/token");

        SalesforceOAuthProperties properties = new SalesforceOAuthProperties(tokenUrl,
                "test-client", "test-secret");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceOAuthClient client = new SalesforceOAuthClient(builder, properties);

        server.expect(requestTo(tokenUrl)).andExpect(method(HttpMethod.POST)).andRespond(
                withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("""
                        {
                          "error": "invalid_client",
                          "error_description": "credentials rejected"
                        }
                        """));

        assertThatThrownBy(client::authenticate)
                .isInstanceOfSatisfying(SalesforceAuthenticationException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(400);
                    assertThat(exception).hasMessageNotContaining("credentials rejected")
                            .hasMessageNotContaining("test-secret");
                });

        server.verify();
    }

    @Test
    void rejectsMalformedTokenResponse() {
        URI tokenUrl = URI.create("https://auth.example.test/services/oauth2/token");

        SalesforceOAuthProperties properties = new SalesforceOAuthProperties(tokenUrl,
                "test-client", "test-secret");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceOAuthClient client = new SalesforceOAuthClient(builder, properties);

        server.expect(requestTo(tokenUrl)).andExpect(method(HttpMethod.POST)).andRespond(
                withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("""
                        {
                          "error": "invalid_client",
                          "error_description": "credentials rejected"

                        """));

        assertThatThrownBy(client::authenticate)
                .isInstanceOfSatisfying(SalesforceAuthenticationException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(200);
                    assertThat(exception).hasMessageNotContaining("credentials rejected")
                            .hasMessageNotContaining("test-secret")
                            .hasMessage("Salesforce returned an invalid authentication response");
                });

        server.verify();
    }

    @Test
    void rejectsIncompleteTokenResponse() {
        URI tokenUrl = URI.create("https://auth.example.test/services/oauth2/token");

        SalesforceOAuthProperties properties = new SalesforceOAuthProperties(tokenUrl,
                "test-client", "test-secret");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceOAuthClient client = new SalesforceOAuthClient(builder, properties);

        server.expect(requestTo(tokenUrl)).andExpect(method(HttpMethod.POST)).andRespond(
                withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("""
                        {}
                        """));

        assertThatThrownBy(client::authenticate)
                .isInstanceOfSatisfying(SalesforceAuthenticationException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(200);
                    assertThat(exception).hasMessageNotContaining("credentials rejected")
                            .hasMessageNotContaining("test-secret")
                            .hasMessage("Salesforce returned an invalid authentication response");
                });

        server.verify();
    }

    @Test
    void rejectsAuthenticationServerError() {
        URI tokenUrl = URI.create("https://auth.example.test/services/oauth2/token");

        SalesforceOAuthProperties properties = new SalesforceOAuthProperties(tokenUrl,
                "test-client", "test-secret");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceOAuthClient client = new SalesforceOAuthClient(builder, properties);

        server.expect(requestTo(tokenUrl)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON).body("""
                                {"error":"server_error"}
                                """));

        assertThatThrownBy(client::authenticate)
                .isInstanceOfSatisfying(SalesforceAuthenticationException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(503);
                    assertThat(exception).hasMessageNotContaining("credentials rejected")
                            .hasMessageNotContaining("test-secret")
                            .hasMessage("Salesforce authentication server failed");
                });

        server.verify();
    }

    @Test
    void reportsUnavailableAuthenticationServer() {
        URI tokenUrl = URI.create("https://auth.example.test/services/oauth2/token");

        SalesforceOAuthProperties properties = new SalesforceOAuthProperties(tokenUrl,
                "test-client", "test-secret");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceOAuthClient client = new SalesforceOAuthClient(builder, properties);

        server.expect(requestTo(tokenUrl)).andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    throw new IOException("connection refused");
                });

        assertThatThrownBy(client::authenticate)
                .isInstanceOf(SalesforceAuthenticationUnavailableException.class)
                .hasMessage("Salesforce authentication server is unavailable")
                .hasMessageNotContaining("test-secret");

        server.verify();
    }
}
