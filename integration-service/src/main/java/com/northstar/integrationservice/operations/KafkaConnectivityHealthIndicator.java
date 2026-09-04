package com.northstar.integrationservice.operations;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.core.KafkaAdminOperations;
import org.springframework.stereotype.Component;

@Component("kafkaHealthIndicator")
public class KafkaConnectivityHealthIndicator implements HealthIndicator {

    private final KafkaAdminOperations kafkaAdmin;

    public KafkaConnectivityHealthIndicator(KafkaAdminOperations kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try {
            String clusterId = kafkaAdmin.clusterId();
            return clusterId != null && !clusterId.isBlank()
                    ? Health.up().build()
                    : Health.down().build();
        } catch (RuntimeException exception) {
            return Health.down().build();
        }
    }
}
