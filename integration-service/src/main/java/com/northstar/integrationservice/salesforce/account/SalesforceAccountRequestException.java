package com.northstar.integrationservice.salesforce.account;

import org.springframework.http.HttpStatusCode;

public class SalesforceAccountRequestException extends RuntimeException {
    private final HttpStatusCode statusCode;

    public SalesforceAccountRequestException(HttpStatusCode statusCode) {
        super("Salesforce Account request failed");
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
