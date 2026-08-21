package com.northstar.integrationservice.salesforce.oauth;

public class SalesforceAuthenticationException extends RuntimeException {
    private final int statusCode;

    public SalesforceAuthenticationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
