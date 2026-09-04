package com.northstar.mockerp.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.kafka.core.KafkaAdminOperations;

class KafkaConnectivityHealthIndicatorTest {

    private final KafkaAdminOperations kafkaAdmin = mock(KafkaAdminOperations.class);
    private final KafkaConnectivityHealthIndicator healthIndicator = new KafkaConnectivityHealthIndicator(
            kafkaAdmin);

    @Test
    void reportsUpWhenKafkaResponds() {
        when(kafkaAdmin.clusterId()).thenReturn("test-cluster-id");

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).isEmpty();
        assertThat(health.toString()).doesNotContain("test-cluster-id");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void reportsDownWhenKafkaReturnsNoClusterId(String clusterId) {
        when(kafkaAdmin.clusterId()).thenReturn(clusterId);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).isEmpty();
    }

    @Test
    void reportsDownWithoutExposingFailureDetails() {
        when(kafkaAdmin.clusterId())
                .thenThrow(new IllegalStateException("sensitive broker address and failure"));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).isEmpty();
        assertThat(health.toString()).doesNotContain("sensitive broker address and failure");
    }
}
