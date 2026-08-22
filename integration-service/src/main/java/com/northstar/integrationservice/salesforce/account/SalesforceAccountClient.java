package com.northstar.integrationservice.salesforce.account;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.northstar.integrationservice.salesforce.SalesforceSession;
import com.northstar.integrationservice.salesforce.config.SalesforceApiProperties;

@Component
public class SalesforceAccountClient {
    private final RestClient restClient;
    private final SalesforceApiProperties properties;

    public SalesforceAccountClient(RestClient.Builder restClientBuilder,
            SalesforceApiProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    public SalesforceAccountResponse fetchAccount(SalesforceSession session,
            String salesforceAccountId) {

        if (salesforceAccountId == null
                || (salesforceAccountId.length() != 15 && salesforceAccountId.length() != 18)
                || !salesforceAccountId.matches("^[A-Za-z0-9]+$")) {
            throw new IllegalArgumentException("Invalid Salesforce account Id");
        }

        String query = "SELECT Id, Name, Business_ID__c, BillingCity " + "FROM Account WHERE Id = '"
                + salesforceAccountId + "'";

        URI uri = UriComponentsBuilder.fromUri(session.getInstanceUrl())
                .pathSegment("services", "data", properties.version(), "query")
                .queryParam("q", query).build().encode().toUri();

        try {
            SalesforceQueryResponse response = restClient.get().uri(uri)
                    .header(HttpHeaders.AUTHORIZATION,
                            session.getTokenType() + " " + session.getAccessToken())
                    .retrieve().onStatus(status -> status.isError(), (request, remoteResponse) -> {
                        throw new SalesforceAccountRequestException(remoteResponse.getStatusCode());
                    }).body(SalesforceQueryResponse.class);

            if (response == null || response.getRecords() == null) {
                throw new SalesforceAccountResponseException();
            }

            if (response.getTotalSize() == 0 && response.getRecords().isEmpty()) {
                throw new SalesforceAccountNotFoundException(salesforceAccountId);
            }

            if (!response.isDone() || response.getTotalSize() != 1
                    || response.getRecords().size() != 1
                    || response.getRecords().getFirst() == null) {
                throw new SalesforceAccountResponseException();
            }

            return response.getRecords().getFirst();
        } catch (ResourceAccessException exception) {
            throw new SalesforceAccountUnavailableException();
        } catch (RestClientException exception) {
            if (exception.getCause() instanceof HttpMessageNotReadableException) {
                throw new SalesforceAccountResponseException();
            }

            throw exception;
        }
    }
}
