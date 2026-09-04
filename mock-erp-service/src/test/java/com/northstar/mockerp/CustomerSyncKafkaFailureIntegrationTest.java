package com.northstar.mockerp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.northstar.mockerp.application.customer.CustomerSyncEventHandler;
import com.northstar.mockerp.messaging.config.CustomerSyncKafkaErrorConfiguration;
import com.northstar.mockerp.messaging.customer.CustomerSyncPayload;
import com.northstar.mockerp.messaging.customer.CustomerSyncRequestedEvent;
import com.northstar.mockerp.persistence.customer.ErpCustomerRepository;
import com.northstar.mockerp.persistence.customer.ErpProcessedCustomerSyncEventEntityRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {"northstar.messaging.customer-sync.listener-enabled=true",
        "northstar.messaging.customer-sync.topic=customer-sync-failure-test",
        "northstar.messaging.customer-sync.group-id=customer-sync-failure-test-group",
        "northstar.messaging.customer-sync.retry-backoff=10ms"})
@EmbeddedKafka(partitions = 1, topics = {"customer-sync-failure-test",
        "customer-sync-failure-test.DLT"}, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@Import(PostgreSqlTestConfiguration.class)
class CustomerSyncKafkaFailureIntegrationTest {

    private static final String TOPIC = "customer-sync-failure-test";
    private static final String DLT_TOPIC = TOPIC + ".DLT";
    private static final String SOURCE_CUSTOMER_ID = "001ABC123456789";
    private static final String MESSAGE_KEY = "NORTHSTAR-001";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final EmbeddedKafkaBroker embeddedKafkaBroker;
    private final ErpCustomerRepository repository;
    private final ErpProcessedCustomerSyncEventEntityRepository processedEventRepository;

    @MockitoSpyBean
    private CustomerSyncEventHandler eventHandler;

    @Autowired
    CustomerSyncKafkaFailureIntegrationTest(KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper, EmbeddedKafkaBroker embeddedKafkaBroker,
            ErpCustomerRepository repository,
            ErpProcessedCustomerSyncEventEntityRepository processedEventRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.embeddedKafkaBroker = embeddedKafkaBroker;
        this.repository = repository;
        this.processedEventRepository = processedEventRepository;
    }

    @BeforeEach
    void clearCustomers() {
        processedEventRepository.deleteAll();
        repository.deleteAll();
    }

    @Test
    void routesPermanentValidationFailureToDeadLetterTopicWithoutRetrying() throws Exception {
        CustomerSyncRequestedEvent event = eventWithBusinessId(null);

        try (Consumer<String, String> deadLetterConsumer = createDeadLetterConsumer()) {
            publish(event);

            ConsumerRecord<String, String> deadLetterRecord = KafkaTestUtils
                    .getSingleRecord(deadLetterConsumer, DLT_TOPIC, Duration.ofSeconds(10));

            assertDeadLetterRecord(deadLetterRecord, event, "VALIDATION");
            verify(eventHandler, timeout(10_000).times(1)).handle(MESSAGE_KEY, event);
            verifyNoMoreInteractions(eventHandler);
            assertNoDataWasPersisted(event);
        }
    }

    @Test
    void routesExhaustedRetryableFailureToDeadLetterTopicAfterThreeAttempts() throws Exception {
        CustomerSyncRequestedEvent event = eventWithBusinessId(MESSAGE_KEY);
        doThrow(new IllegalStateException("simulated temporary database failure"))
                .when(eventHandler).handle(anyString(), any(CustomerSyncRequestedEvent.class));

        try (Consumer<String, String> deadLetterConsumer = createDeadLetterConsumer()) {
            publish(event);

            ConsumerRecord<String, String> deadLetterRecord = KafkaTestUtils
                    .getSingleRecord(deadLetterConsumer, DLT_TOPIC, Duration.ofSeconds(10));

            assertDeadLetterRecord(deadLetterRecord, event, "RETRY_EXHAUSTED");
            verify(eventHandler, timeout(10_000).times(3)).handle(MESSAGE_KEY, event);
            verifyNoMoreInteractions(eventHandler);
            assertNoDataWasPersisted(event);
        }
    }

    private Consumer<String, String> createDeadLetterConsumer() {
        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(embeddedKafkaBroker,
                "customer-sync-failure-dlt-observer-" + UUID.randomUUID(), false);
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(consumerProperties,
                new StringDeserializer(), new StringDeserializer()).createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, true, DLT_TOPIC);
        return consumer;
    }

    private void publish(CustomerSyncRequestedEvent event) throws Exception {
        kafkaTemplate.send(TOPIC, MESSAGE_KEY, event).get(10, TimeUnit.SECONDS);
    }

    private void assertDeadLetterRecord(ConsumerRecord<String, String> record,
            CustomerSyncRequestedEvent originalEvent, String expectedFailureCategory)
            throws Exception {
        assertThat(record.topic()).isEqualTo(DLT_TOPIC);
        assertThat(record.key()).isEqualTo(MESSAGE_KEY);
        assertThat(objectMapper.readValue(record.value(), CustomerSyncRequestedEvent.class))
                .isEqualTo(originalEvent);
        assertThat(headerValue(record, CustomerSyncKafkaErrorConfiguration.FAILURE_CATEGORY_HEADER))
                .isEqualTo(expectedFailureCategory);
        assertThat(record.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE)).isNull();
        assertThat(record.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_STACKTRACE)).isNull();
    }

    private String headerValue(ConsumerRecord<String, String> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        assertThat(header).isNotNull();
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private void assertNoDataWasPersisted(CustomerSyncRequestedEvent event) {
        assertThat(repository.findBySourceCustomerId(SOURCE_CUSTOMER_ID)).isEmpty();
        assertThat(processedEventRepository.existsById(event.eventId())).isFalse();
    }

    private CustomerSyncRequestedEvent eventWithBusinessId(String businessId) {
        return new CustomerSyncRequestedEvent(UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-09-04T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                new CustomerSyncPayload(SOURCE_CUSTOMER_ID, businessId, "Designated Test Account",
                        "Helsinki"));
    }
}
