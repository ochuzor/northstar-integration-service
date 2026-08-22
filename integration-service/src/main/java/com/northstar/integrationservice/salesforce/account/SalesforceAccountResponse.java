package com.northstar.integrationservice.salesforce.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SalesforceAccountResponse(@JsonProperty("Id") String id,
        @JsonProperty("Name") String name, @JsonProperty("Business_ID__c") String businessId,
        @JsonProperty("BillingCity") String billingCity) {
}
