package com.northstar.integrationservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import com.northstar.integrationservice.messaging.customer.CustomerSyncEventPublisher;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPayload;
import com.northstar.integrationservice.messaging.customer.CustomerSyncRequestedEvent;

@SpringBootTest(properties = {
        "salesforce.oauth.token-url=https://auth.example.test/services/oauth2/token",
        "salesforce.oauth.client-id=test-client", "salesforce.oauth.client-secret=test-secret",
        "salesforce.api.version=v66.0"})
@EmbeddedKafka(partitions = 1, topics = "northstar.customer-sync.v1", bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class CustomerSyncEventPublisherIntegrationTest {
    private static final String TOPIC = "northstar.customer-sync.v1";

    @Autowired
    private CustomerSyncEventPublisher publisher;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void publishesCustomerSyncEventWithBusinessIdAsKey() {
        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID correlationId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Instant occurredAt = Instant.parse("2026-08-29T10:15:30Z");
        CustomerSyncPayload payload = new CustomerSyncPayload("001ABC123456789", "NORTHSTAR-001",
                "Designated Test Account", "Helsinki");
        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(eventId, correlationId,
                occurredAt, 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE", payload);

        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(embeddedKafkaBroker,
                "customer-sync-publisher-integration-test", false);
        JacksonJsonDeserializer<CustomerSyncRequestedEvent> valueDeserializer = new JacksonJsonDeserializer<>(
                CustomerSyncRequestedEvent.class);

        try (Consumer<String, CustomerSyncRequestedEvent> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProperties, new StringDeserializer(), valueDeserializer).createConsumer()) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, TOPIC);

            publisher.publishEvent(event);

            ConsumerRecord<String, CustomerSyncRequestedEvent> record = KafkaTestUtils
                    .getSingleRecord(consumer, TOPIC, Duration.ofSeconds(10));

            assertThat(record.topic()).isEqualTo(TOPIC);
            assertThat(record.key()).isEqualTo("NORTHSTAR-001");
            assertThat(record.value()).isEqualTo(event);
        }
    }
}
