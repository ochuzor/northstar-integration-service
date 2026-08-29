package com.northstar.integrationservice.messaging.customer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.northstar.integrationservice.messaging.config.CustomerSyncMessagingProperties;

@Component
public class CustomerSyncEventPublisher {
    private final KafkaTemplate<String, CustomerSyncRequestedEvent> kafkaTemplate;
    private final CustomerSyncMessagingProperties properties;

    public CustomerSyncEventPublisher(
            KafkaTemplate<String, CustomerSyncRequestedEvent> kafkaTemplate,
            CustomerSyncMessagingProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public CustomerSyncPublicationResult publishEvent(CustomerSyncRequestedEvent event) {
        String topic = properties.topic();
        String businessId = event.customer().businessId();

        try {
            kafkaTemplate.send(topic, businessId, event)
                    .get(properties.acknowledgementTimeout().toMillis(), TimeUnit.MILLISECONDS);

            return new CustomerSyncPublicationResult(event.eventId(), event.correlationId(),
                    event.customer().sourceCustomerId());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CustomerSyncPublicationException();
        } catch (ExecutionException | TimeoutException | RuntimeException exception) {
            throw new CustomerSyncPublicationException();
        }
    }
}
