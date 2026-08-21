package com.northstar.integrationservice.salesforce.oauth;

public class SalesforceAuthenticationUnavailableException extends RuntimeException {
    public SalesforceAuthenticationUnavailableException() {
        super("Salesforce authentication server is unavailable");
    }
}
