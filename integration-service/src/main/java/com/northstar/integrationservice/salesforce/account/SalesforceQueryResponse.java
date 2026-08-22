package com.northstar.integrationservice.salesforce.account;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SalesforceQueryResponse {
    private final int totalSize;
    private final boolean done;
    private final List<SalesforceAccountResponse> records;

    @JsonCreator
    public SalesforceQueryResponse(int totalSize, boolean done,
            List<SalesforceAccountResponse> records) {
        this.totalSize = totalSize;
        this.done = done;
        this.records = records;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public boolean isDone() {
        return done;
    }

    public List<SalesforceAccountResponse> getRecords() {
        return records;
    }
}
