package com.northstar.integrationservice.salesforce.account;

public class SalesforceAccountNotFoundException extends RuntimeException {
    public SalesforceAccountNotFoundException(String accountId) {
        super("Salesforce Account " + accountId + " was not found");
    }
}
