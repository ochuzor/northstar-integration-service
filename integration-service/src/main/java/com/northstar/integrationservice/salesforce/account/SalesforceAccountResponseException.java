package com.northstar.integrationservice.salesforce.account;

public class SalesforceAccountResponseException extends RuntimeException {
    public SalesforceAccountResponseException() {
        super("Invalid Salesforce query response");
    }
}
