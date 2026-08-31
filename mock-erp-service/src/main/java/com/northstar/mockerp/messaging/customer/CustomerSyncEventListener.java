package com.northstar.mockerp.messaging.customer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.northstar.mockerp.application.customer.CustomerSyncEventHandler;

@Component
public class CustomerSyncEventListener {
    private final CustomerSyncEventHandler eventHandler;

    public CustomerSyncEventListener(CustomerSyncEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @KafkaListener(topics = "${northstar.messaging.customer-sync.topic}", groupId = "${northstar.messaging.customer-sync.group-id}", autoStartup = "${northstar.messaging.customer-sync.listener-enabled}")
    public void consume(ConsumerRecord<String, CustomerSyncRequestedEvent> record) {
        eventHandler.handle(record.key(), record.value());
    }
}
