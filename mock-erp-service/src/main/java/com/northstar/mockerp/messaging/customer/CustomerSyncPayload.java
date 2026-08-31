package com.northstar.mockerp.messaging.customer;

public record CustomerSyncPayload(String sourceCustomerId, String businessId, String name,
        String billingCity) {
}
