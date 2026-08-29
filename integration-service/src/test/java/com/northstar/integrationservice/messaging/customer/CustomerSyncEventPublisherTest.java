package com.northstar.integrationservice.messaging.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.northstar.integrationservice.messaging.config.CustomerSyncMessagingProperties;

@ExtendWith(MockitoExtension.class)
class CustomerSyncEventPublisherTest {

    private static final String TOPIC = "northstar.customer-sync.v1";
    private static final String BUSINESS_ID = "NORTHSTAR-001";

    @Mock
    private KafkaTemplate<String, CustomerSyncRequestedEvent> kafkaTemplate;

    private CustomerSyncEventPublisher publisher;

    @BeforeEach
    void setUp() {
        CustomerSyncMessagingProperties properties = new CustomerSyncMessagingProperties(TOPIC,
                Duration.ofSeconds(5));

        publisher = new CustomerSyncEventPublisher(kafkaTemplate, properties);
    }

    @Test
    void publishesEventWithConfiguredTopicAndBusinessIdKey() {
        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID correlationId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        CustomerSyncPayload payload = new CustomerSyncPayload("001ABC123456789", BUSINESS_ID,
                "Designated Test Account", "Helsinki");

        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(eventId, correlationId,
                Instant.parse("2026-08-29T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                payload);

        CompletableFuture<SendResult<String, CustomerSyncRequestedEvent>> acknowledgedSend = CompletableFuture
                .completedFuture(null);

        when(kafkaTemplate.send(TOPIC, BUSINESS_ID, event)).thenReturn(acknowledgedSend);

        CustomerSyncPublicationResult result = publisher.publishEvent(event);

        assertThat(result.eventId()).isEqualTo(eventId);
        assertThat(result.correlationId()).isEqualTo(correlationId);
        assertThat(result.sourceCustomerId()).isEqualTo("001ABC123456789");

        verify(kafkaTemplate).send(TOPIC, BUSINESS_ID, event);
    }

    @Test
    void throwsPublicationExceptionWhenKafkaRejectsSend() {
        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID correlationId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        CustomerSyncPayload payload = new CustomerSyncPayload("001ABC123456789", BUSINESS_ID,
                "Designated Test Account", "Helsinki");

        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(eventId, correlationId,
                Instant.parse("2026-08-29T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                payload);

        CompletableFuture<SendResult<String, CustomerSyncRequestedEvent>> rejectedSend = CompletableFuture
                .failedFuture(new RuntimeException("Private broker failure detail"));

        when(kafkaTemplate.send(TOPIC, BUSINESS_ID, event)).thenReturn(rejectedSend);

        assertThatThrownBy(() -> publisher.publishEvent(event))
                .isInstanceOf(CustomerSyncPublicationException.class)
                .hasMessage("Customer sync event publication failed");
    }

    @Test
    void throwsPublicationExceptionWhenAcknowledgementTimesOut() {
        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID correlationId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        CustomerSyncPayload payload = new CustomerSyncPayload("001ABC123456789", BUSINESS_ID,
                "Designated Test Account", "Helsinki");

        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(eventId, correlationId,
                Instant.parse("2026-08-29T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                payload);

        CustomerSyncMessagingProperties shortTimeoutProperties = new CustomerSyncMessagingProperties(
                TOPIC, Duration.ofMillis(10));

        CustomerSyncEventPublisher shortTimeoutPublisher = new CustomerSyncEventPublisher(
                kafkaTemplate, shortTimeoutProperties);

        CompletableFuture<SendResult<String, CustomerSyncRequestedEvent>> pendingSend = new CompletableFuture<>();

        when(kafkaTemplate.send(TOPIC, BUSINESS_ID, event)).thenReturn(pendingSend);

        assertThatThrownBy(() -> shortTimeoutPublisher.publishEvent(event))
                .isInstanceOf(CustomerSyncPublicationException.class)
                .hasMessage("Customer sync event publication failed");
    }

    @Test
    void restoresInterruptFlagWhenPublicationIsInterrupted() {
        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID correlationId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        CustomerSyncPayload payload = new CustomerSyncPayload("001ABC123456789", BUSINESS_ID,
                "Designated Test Account", "Helsinki");

        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(eventId, correlationId,
                Instant.parse("2026-08-29T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                payload);

        CompletableFuture<SendResult<String, CustomerSyncRequestedEvent>> interruptedSend = new CompletableFuture<>() {
            @Override
            public SendResult<String, CustomerSyncRequestedEvent> get(long timeout, TimeUnit unit)
                    throws InterruptedException {
                throw new InterruptedException("Test interruption");
            }
        };

        when(kafkaTemplate.send(TOPIC, BUSINESS_ID, event)).thenReturn(interruptedSend);

        assertThat(Thread.currentThread().isInterrupted()).isFalse();

        try {
            assertThatThrownBy(() -> publisher.publishEvent(event))
                    .isInstanceOf(CustomerSyncPublicationException.class)
                    .hasMessage("Customer sync event publication failed");

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

    }
}
