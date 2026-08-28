package com.northstar.integrationservice.messaging.config;

import java.time.Clock;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.northstar.integrationservice.messaging.customer.CustomerSyncEventFactory;
import com.northstar.integrationservice.messaging.customer.UuidGenerator;

@Configuration(proxyBeanMethods = false)
public class MessagingConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    UuidGenerator uuidGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    CustomerSyncEventFactory customerSyncEventFactory(Clock clock, UuidGenerator uuidGenerator) {
        return new CustomerSyncEventFactory(clock, uuidGenerator);
    }
}
