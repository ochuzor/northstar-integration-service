package com.northstar.mockerp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import com.northstar.mockerp.messaging.customer.CustomerSyncPayload;
import com.northstar.mockerp.messaging.customer.CustomerSyncRequestedEvent;
import com.northstar.mockerp.persistence.customer.ErpCustomerRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {"northstar.messaging.customer-sync.listener-enabled=true",
        "northstar.messaging.customer-sync.topic=customer-sync-database-test",
        "northstar.messaging.customer-sync.group-id=customer-sync-database-test-group",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"})
@EmbeddedKafka(partitions = 1, topics = "customer-sync-database-test", bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@Import(PostgreSqlTestConfiguration.class)
class CustomerSyncKafkaToDatabaseIntegrationTest {

    private static final String TOPIC = "customer-sync-database-test";
    private static final String SOURCE_CUSTOMER_ID = "001ABC123456789";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ErpCustomerRepository repository;

    @Autowired
    CustomerSyncKafkaToDatabaseIntegrationTest(KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper, ErpCustomerRepository repository) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    @Test
    void consumesCustomerSyncEventAndPersistsCustomer() throws Exception {
        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-09-03T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                new CustomerSyncPayload(SOURCE_CUSTOMER_ID, "NORTHSTAR-001",
                        "Designated Test Account", "Helsinki"));
        String json = objectMapper.writeValueAsString(event);

        kafkaTemplate.send(TOPIC, event.customer().businessId(), json).get(10, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(
                () -> assertThat(repository.findBySourceCustomerId(SOURCE_CUSTOMER_ID))
                        .hasValueSatisfying(customer -> {
                            assertThat(customer.getId()).isNotNull();
                            assertThat(customer.getSourceCustomerId())
                                    .isEqualTo(SOURCE_CUSTOMER_ID);
                            assertThat(customer.getBusinessId()).isEqualTo("NORTHSTAR-001");
                            assertThat(customer.getName()).isEqualTo("Designated Test Account");
                            assertThat(customer.getBillingCity()).isEqualTo("Helsinki");
                        }));
    }
}
