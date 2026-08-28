package com.northstar.integrationservice.messaging.customer;

import java.util.UUID;

@FunctionalInterface
public interface UuidGenerator {
    UUID generate();
}
