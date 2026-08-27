package com.northstar.integrationservice.web.account;

public record AccountSyncResponse(String salesforceAccountId, String name, String businessId,
        String billingCity) {
}
