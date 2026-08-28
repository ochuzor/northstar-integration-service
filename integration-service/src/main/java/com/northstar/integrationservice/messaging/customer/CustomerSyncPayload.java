package com.northstar.integrationservice.messaging.customer;

public record CustomerSyncPayload(String sourceCustomerId, String businessId, String name,
        String billingCity) {
}
