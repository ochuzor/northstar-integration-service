package com.northstar.mockerp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.northstar.mockerp.application.customer.CustomerSyncEventHandler;
import com.northstar.mockerp.messaging.customer.CustomerSyncPayload;
import com.northstar.mockerp.messaging.customer.CustomerSyncRequestedEvent;
import com.northstar.mockerp.persistence.customer.ErpCustomerRepository;
import com.northstar.mockerp.persistence.customer.ErpProcessedCustomerSyncEventEntityRepository;

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
    private final ErpProcessedCustomerSyncEventEntityRepository processedEventRepository;

    @MockitoSpyBean
    private CustomerSyncEventHandler eventHandler;

    @Autowired
    CustomerSyncKafkaToDatabaseIntegrationTest(KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper, ErpCustomerRepository repository,
            ErpProcessedCustomerSyncEventEntityRepository processedEventRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.processedEventRepository = processedEventRepository;
    }

    @BeforeEach
    void clearCustomers() {
        processedEventRepository.deleteAll();
        repository.deleteAll();
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

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(repository.findBySourceCustomerId(SOURCE_CUSTOMER_ID))
                    .hasValueSatisfying(customer -> {
                        assertThat(customer.getId()).isNotNull();
                        assertThat(customer.getSourceCustomerId()).isEqualTo(SOURCE_CUSTOMER_ID);
                        assertThat(customer.getBusinessId()).isEqualTo("NORTHSTAR-001");
                        assertThat(customer.getName()).isEqualTo("Designated Test Account");
                        assertThat(customer.getBillingCity()).isEqualTo("Helsinki");
                    });
            assertThat(processedEventRepository.existsById(event.eventId())).isTrue();
        });
    }

    @Test
    void updatesExistingCustomerWhenSameSourceCustomerIsDeliveredAgain() throws Exception {
        CustomerSyncRequestedEvent initialEvent = new CustomerSyncRequestedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-09-03T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                new CustomerSyncPayload(SOURCE_CUSTOMER_ID, "NORTHSTAR-001",
                        "Designated Test Account", "Helsinki"));
        String initialJson = objectMapper.writeValueAsString(initialEvent);

        kafkaTemplate.send(TOPIC, initialEvent.customer().businessId(), initialJson).get(10,
                TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(
                () -> assertThat(repository.findBySourceCustomerId(SOURCE_CUSTOMER_ID))
                        .isPresent());
        Long originalDatabaseId = repository.findBySourceCustomerId(SOURCE_CUSTOMER_ID)
                .orElseThrow().getId();

        CustomerSyncRequestedEvent updatedEvent = new CustomerSyncRequestedEvent(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                Instant.parse("2026-09-03T12:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                new CustomerSyncPayload(SOURCE_CUSTOMER_ID, "NORTHSTAR-002",
                        "Designated Account for testing", "Stockholm"));
        String updatedJson = objectMapper.writeValueAsString(updatedEvent);

        kafkaTemplate.send(TOPIC, initialEvent.customer().businessId(), updatedJson).get(10,
                TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(repository.count()).isEqualTo(1);
            assertThat(processedEventRepository.count()).isEqualTo(2);
            assertThat(processedEventRepository.existsById(initialEvent.eventId())).isTrue();
            assertThat(processedEventRepository.existsById(updatedEvent.eventId())).isTrue();
            assertThat(repository.findBySourceCustomerId(SOURCE_CUSTOMER_ID))
                    .hasValueSatisfying(customer -> {
                        assertThat(customer.getId()).isEqualTo(originalDatabaseId);
                        assertThat(customer.getBusinessId()).isEqualTo("NORTHSTAR-002");
                        assertThat(customer.getName()).isEqualTo("Designated Account for testing");
                        assertThat(customer.getBillingCity()).isEqualTo("Stockholm");
                    });
        });
    }

    @Test
    void skipsExactDuplicateEventWithoutRepeatingPersistence() throws Exception {
        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                Instant.parse("2026-09-03T14:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                new CustomerSyncPayload(SOURCE_CUSTOMER_ID, "NORTHSTAR-001",
                        "Designated Test Account", "Helsinki"));
        String json = objectMapper.writeValueAsString(event);

        kafkaTemplate.send(TOPIC, event.customer().businessId(), json).get(10, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(repository.count()).isEqualTo(1);
            assertThat(processedEventRepository.count()).isEqualTo(1);
        });
        Long originalDatabaseId = repository.findBySourceCustomerId(SOURCE_CUSTOMER_ID)
                .orElseThrow().getId();

        kafkaTemplate.send(TOPIC, event.customer().businessId(), json).get(10, TimeUnit.SECONDS);

        verify(eventHandler, timeout(10_000).times(2)).handle(event.customer().businessId(), event);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(processedEventRepository.count()).isEqualTo(1);
        assertThat(processedEventRepository.existsById(event.eventId())).isTrue();
        assertThat(repository.findBySourceCustomerId(SOURCE_CUSTOMER_ID)).hasValueSatisfying(
                customer -> assertThat(customer.getId()).isEqualTo(originalDatabaseId));

    }
}
