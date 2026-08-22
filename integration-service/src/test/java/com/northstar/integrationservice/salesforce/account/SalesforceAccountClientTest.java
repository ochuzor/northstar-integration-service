package com.northstar.integrationservice.salesforce.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.northstar.integrationservice.salesforce.SalesforceSession;
import com.northstar.integrationservice.salesforce.config.SalesforceApiProperties;

class SalesforceAccountClientTest {
    @Test
    void requestsAccountById() {
        URI instanceUrl = URI.create("https://instance.example.test");
        String accountId = "001ABC123456789";

        SalesforceSession session = new SalesforceSession("synthetic-token", instanceUrl, "Bearer",
                Instant.parse("2026-01-01T00:00:00Z"));

        SalesforceApiProperties properties = new SalesforceApiProperties("v66.0");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        SalesforceAccountClient client = new SalesforceAccountClient(builder, properties);

        String expectedQuery = "SELECT Id, Name, Business_ID__c, BillingCity "
                + "FROM Account WHERE Id = '001ABC123456789'";

        server.expect(request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/services/data/v66.0/query");

            String encodedQuery = UriComponentsBuilder.fromUri(request.getURI()).build()
                    .getQueryParams().getFirst("q");

            String actualQuery = UriUtils.decode(encodedQuery, StandardCharsets.UTF_8);

            assertThat(actualQuery).isEqualTo(expectedQuery);
        }).andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer synthetic-token"))
                .andRespond(withSuccess("""
                        {
                          "totalSize": 1,
                          "done": true,
                          "records": [
                            {
                              "Id": "001ABC123456789",
                              "Name": "Designated Test Account",
                              "Business_ID__c": "NORTHSTAR-001",
                              "BillingCity": "Helsinki"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        SalesforceAccountResponse account = client.fetchAccount(session, accountId);

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.name()).isEqualTo("Designated Test Account");
        assertThat(account.businessId()).isEqualTo("NORTHSTAR-001");
        assertThat(account.billingCity()).isEqualTo("Helsinki");

        server.verify();
    }

    @Test
    void throwsAccountNotFoundWhenQueryReturnsNoRecords() {
        URI instanceUrl = URI.create("https://instance.example.test");
        String accountId = "001ABC123456789";

        SalesforceSession session = new SalesforceSession("synthetic-token", instanceUrl, "Bearer",
                Instant.parse("2026-01-01T00:00:00Z"));

        SalesforceApiProperties properties = new SalesforceApiProperties("v66.0");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceAccountClient client = new SalesforceAccountClient(builder, properties);

        server.expect(request -> assertThat(request.getURI().getPath())
                .isEqualTo("/services/data/v66.0/query")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "totalSize": 0,
                          "done": true,
                          "records": []
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchAccount(session, accountId))
                .isInstanceOf(SalesforceAccountNotFoundException.class)
                .hasMessage("Salesforce Account 001ABC123456789 was not found")
                .hasMessageNotContaining("synthetic-token");

        server.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource // Tests null and ""
    @ValueSource(strings = {"   ", // Blank / Whitespace
            "123", // Too short (incorrect length)
            "12345678901234567890", // Too long (incorrect length)
            "123456789012345*", // Non-alphanumeric (contains asterisk)
            "invalid-id!" // Non-alphanumeric (contains hyphen/bang)
    })
    void rejectsInvalidAccountId(String invalidId) {
        URI instanceUrl = URI.create("https://instance.example.test");

        SalesforceSession session = new SalesforceSession("synthetic-token", instanceUrl, "Bearer",
                Instant.parse("2026-01-01T00:00:00Z"));

        SalesforceApiProperties properties = new SalesforceApiProperties("v66.0");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceAccountClient client = new SalesforceAccountClient(builder, properties);

        assertThatThrownBy(() -> client.fetchAccount(session, invalidId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid Salesforce account Id")
                .hasMessageNotContaining("synthetic-token");

        server.verify();
    }

    @Test
    void rejectsMultipleAccountRecords() {
        URI instanceUrl = URI.create("https://instance.example.test");
        String accountId = "001ABC123456789";

        SalesforceSession session = new SalesforceSession("synthetic-token", instanceUrl, "Bearer",
                Instant.parse("2026-01-01T00:00:00Z"));

        SalesforceApiProperties properties = new SalesforceApiProperties("v66.0");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceAccountClient client = new SalesforceAccountClient(builder, properties);

        server.expect(request -> assertThat(request.getURI().getPath())
                .isEqualTo("/services/data/v66.0/query")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "totalSize": 2,
                          "done": true,
                          "records": [
                            {
                              "Id": "001ABC123456789",
                              "Name": "Designated Test Account#2",
                              "Business_ID__c": "NORTHSTAR-001",
                              "BillingCity": "Helsinki"
                            },
                            {
                              "Id": "001ABC123456780",
                              "Name": "Designated Test Account#2",
                              "Business_ID__c": "NORTHSTAR-002",
                              "BillingCity": "Stockholm"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchAccount(session, accountId))
                .isInstanceOf(SalesforceAccountResponseException.class)
                .hasMessage("Invalid Salesforce query response")
                .hasMessageNotContaining("synthetic-token");

        server.verify();
    }

    @Test
    void rejectsMalformedAccountResponse() {
        URI instanceUrl = URI.create("https://instance.example.test");
        String accountId = "001ABC123456789";

        SalesforceSession session = new SalesforceSession("synthetic-token", instanceUrl, "Bearer",
                Instant.parse("2026-01-01T00:00:00Z"));

        SalesforceApiProperties properties = new SalesforceApiProperties("v66.0");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceAccountClient client = new SalesforceAccountClient(builder, properties);

        server.expect(request -> assertThat(request.getURI().getPath())
                .isEqualTo("/services/data/v66.0/query")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "totalSize": 2,
                          "done": true,
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchAccount(session, accountId))
                .isInstanceOf(SalesforceAccountResponseException.class)
                .hasMessage("Invalid Salesforce query response")
                .hasMessageNotContaining("synthetic-token");

        server.verify();
    }

    @Test
    void rejectsAccountResponseWithoutRecords() {
        URI instanceUrl = URI.create("https://instance.example.test");
        String accountId = "001ABC123456789";

        SalesforceSession session = new SalesforceSession("synthetic-token", instanceUrl, "Bearer",
                Instant.parse("2026-01-01T00:00:00Z"));

        SalesforceApiProperties properties = new SalesforceApiProperties("v66.0");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceAccountClient client = new SalesforceAccountClient(builder, properties);

        server.expect(request -> assertThat(request.getURI().getPath())
                .isEqualTo("/services/data/v66.0/query")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                            "totalSize": 1,
                            "done": true
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchAccount(session, accountId))
                .isInstanceOf(SalesforceAccountResponseException.class)
                .hasMessage("Invalid Salesforce query response")
                .hasMessageNotContaining("synthetic-token");

        server.verify();
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"   ", "{ \"totalSize\": 1, \"done\": true }",
            "{ \"totalSize\": 1, \"done\": false }", """
                       {
                            "totalSize": 1,
                            "done": false,
                            "records": [
                                {
                                "Id": "001ABC123456789",
                                "Name": "Designated Test Account#2",
                                "Business_ID__c": "NORTHSTAR-001",
                                "BillingCity": "Helsinki"
                                }
                            ]
                        }
                    """})
    void rejectsInvalidAccountResponseData(String responseText) {
        URI instanceUrl = URI.create("https://instance.example.test");
        String accountId = "001ABC123456789";

        SalesforceSession session = new SalesforceSession("synthetic-token", instanceUrl, "Bearer",
                Instant.parse("2026-01-01T00:00:00Z"));

        SalesforceApiProperties properties = new SalesforceApiProperties("v66.0");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceAccountClient client = new SalesforceAccountClient(builder, properties);

        server.expect(request -> assertThat(request.getURI().getPath())
                .isEqualTo("/services/data/v66.0/query")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseText, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchAccount(session, accountId))
                .isInstanceOf(SalesforceAccountResponseException.class)
                .hasMessage("Invalid Salesforce query response")
                .hasMessageNotContaining("synthetic-token");

        server.verify();
    }

    @Test
    void rejectsEmptyAccountResponse() {
        URI instanceUrl = URI.create("https://instance.example.test");
        String accountId = "001ABC123456789";

        SalesforceSession session = new SalesforceSession("synthetic-token", instanceUrl, "Bearer",
                Instant.parse("2026-01-01T00:00:00Z"));

        SalesforceApiProperties properties = new SalesforceApiProperties("v66.0");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceAccountClient client = new SalesforceAccountClient(builder, properties);

        server.expect(request -> assertThat(request.getURI().getPath())
                .isEqualTo("/services/data/v66.0/query")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> client.fetchAccount(session, accountId))
                .isInstanceOf(SalesforceAccountResponseException.class)
                .hasMessage("Invalid Salesforce query response")
                .hasMessageNotContaining("synthetic-token");

        server.verify();
    }

    @Test
    void rejectsUnauthorizedAccountRequest() {
        URI instanceUrl = URI.create("https://instance.example.test");
        String accountId = "001ABC123456789";

        SalesforceSession session = new SalesforceSession("synthetic-token", instanceUrl, "Bearer",
                Instant.parse("2026-01-01T00:00:00Z"));

        SalesforceApiProperties properties = new SalesforceApiProperties("v66.0");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceAccountClient client = new SalesforceAccountClient(builder, properties);

        server.expect(request -> assertThat(request.getURI().getPath())
                .isEqualTo("/services/data/v66.0/query")).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.fetchAccount(session, accountId))
                .isInstanceOfSatisfying(SalesforceAccountRequestException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception).hasMessage("Salesforce Account request failed");
                    assertThat(exception).hasMessageNotContaining("synthetic-token");
                    assertThat(exception).hasMessageNotContaining("instance.example.test");
                });

        server.verify();
    }

    @Test
    void rejectsThrottledAccountRequest() {
        URI instanceUrl = URI.create("https://instance.example.test");
        String accountId = "001ABC123456789";

        SalesforceSession session = new SalesforceSession("synthetic-token", instanceUrl, "Bearer",
                Instant.parse("2026-01-01T00:00:00Z"));

        SalesforceApiProperties properties = new SalesforceApiProperties("v66.0");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceAccountClient client = new SalesforceAccountClient(builder, properties);

        server.expect(request -> assertThat(request.getURI().getPath())
                .isEqualTo("/services/data/v66.0/query")).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.fetchAccount(session, accountId))
                .isInstanceOfSatisfying(SalesforceAccountRequestException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(exception).hasMessage("Salesforce Account request failed");
                    assertThat(exception).hasMessageNotContaining("synthetic-token");
                    assertThat(exception).hasMessageNotContaining("instance.example.test");
                });

        server.verify();
    }

    @Test
    void rejectsSalesforceAccountServerError() {
        URI instanceUrl = URI.create("https://instance.example.test");
        String accountId = "001ABC123456789";

        SalesforceSession session = new SalesforceSession("synthetic-token", instanceUrl, "Bearer",
                Instant.parse("2026-01-01T00:00:00Z"));

        SalesforceApiProperties properties = new SalesforceApiProperties("v66.0");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceAccountClient client = new SalesforceAccountClient(builder, properties);

        server.expect(request -> assertThat(request.getURI().getPath())
                .isEqualTo("/services/data/v66.0/query")).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.fetchAccount(session, accountId))
                .isInstanceOfSatisfying(SalesforceAccountRequestException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(exception).hasMessage("Salesforce Account request failed");
                    assertThat(exception).hasMessageNotContaining("synthetic-token");
                    assertThat(exception).hasMessageNotContaining("instance.example.test");
                });

        server.verify();
    }

    @Test
    void reportsUnavailableSalesforceAccountApi() {
        URI instanceUrl = URI.create("https://instance.example.test");
        String accountId = "001ABC123456789";

        SalesforceSession session = new SalesforceSession("synthetic-token", instanceUrl, "Bearer",
                Instant.parse("2026-01-01T00:00:00Z"));

        SalesforceApiProperties properties = new SalesforceApiProperties("v66.0");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SalesforceAccountClient client = new SalesforceAccountClient(builder, properties);

        server.expect(request -> assertThat(request.getURI().getPath())
                .isEqualTo("/services/data/v66.0/query")).andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new ResourceAccessException("Access denied");
                });

        assertThatThrownBy(() -> client.fetchAccount(session, accountId))
                .isInstanceOfSatisfying(SalesforceAccountUnavailableException.class, exception -> {
                    assertThat(exception).hasMessage("Salesforce Account API is unavailable");
                    assertThat(exception).hasMessageNotContaining("synthetic-token");
                    assertThat(exception).hasMessageNotContaining("instance.example.test");
                    assertThat(exception).hasMessageNotContaining("Access denied");
                });

        server.verify();
    }
}
