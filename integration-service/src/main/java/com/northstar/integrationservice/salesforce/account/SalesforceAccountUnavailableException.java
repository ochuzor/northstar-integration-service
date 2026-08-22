package com.northstar.integrationservice.salesforce.account;

public class SalesforceAccountUnavailableException extends RuntimeException {
    public SalesforceAccountUnavailableException() {
        super("Salesforce Account API is unavailable");
    }
}
