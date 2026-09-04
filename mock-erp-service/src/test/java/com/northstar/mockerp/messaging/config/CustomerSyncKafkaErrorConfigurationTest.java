package com.northstar.mockerp.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.FixedBackOff;

import com.northstar.mockerp.domain.customer.ErpCustomerValidationException;

class CustomerSyncKafkaErrorConfigurationTest {

    @Test
    void configuresThreeTotalAttemptsWithFixedBackOff() {
        CustomerSyncConsumerProperties properties = new CustomerSyncConsumerProperties("topic",
                "group", false, 3, Duration.ofSeconds(1), ".DLT");

        FixedBackOff backOff = new CustomerSyncKafkaErrorConfiguration()
                .customerSyncRetryBackOff(properties);
        BackOffExecution execution = backOff.start();

        assertThat(execution.nextBackOff()).isEqualTo(1_000L);
        assertThat(execution.nextBackOff()).isEqualTo(1_000L);
        assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
    }

    @Test
    void configuresValidationFailureAsNonRetryable() {
        DeadLetterPublishingRecoverer recoverer = mock(DeadLetterPublishingRecoverer.class);
        DefaultErrorHandler errorHandler = new CustomerSyncKafkaErrorConfiguration()
                .customerSyncErrorHandler(recoverer, new FixedBackOff(1_000L, 2L));
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "key",
                "value");
        ErpCustomerValidationException failure = new ErpCustomerValidationException(
                Set.of("businessId"));

        assertThat(errorHandler.handleOne(failure, record, null, null)).isTrue();

        verify(recoverer).accept(record, null, failure);
    }

    @SuppressWarnings("unchecked")
    @Test
    void routesRecoveredRecordToSourceDeadLetterTopicWithoutUnsafeHeaders() {
        KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);
        SendResult<Object, Object> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
        CustomerSyncConsumerProperties properties = new CustomerSyncConsumerProperties("topic",
                "group", false, 3, Duration.ofSeconds(1), ".DLT");
        DeadLetterPublishingRecoverer recoverer = new CustomerSyncKafkaErrorConfiguration()
                .customerSyncDeadLetterRecoverer(kafkaTemplate, properties);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 2, 7L, "key",
                "value");

        recoverer.accept(record, null, new IllegalStateException("sensitive details"));

        ArgumentCaptor<ProducerRecord<Object, Object>> publishedRecord = ArgumentCaptor
                .forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(publishedRecord.capture());
        assertThat(publishedRecord.getValue().topic()).isEqualTo("topic.DLT");
        assertThat(publishedRecord.getValue().partition()).isEqualTo(2);
        assertThat(publishedRecord.getValue().key()).isEqualTo("key");
        assertThat(publishedRecord.getValue().value()).isEqualTo("value");
        assertThat(
                publishedRecord.getValue().headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE))
                .isNull();
        assertThat(publishedRecord.getValue().headers()
                .lastHeader(KafkaHeaders.DLT_EXCEPTION_STACKTRACE)).isNull();
        assertThat(new String(publishedRecord.getValue().headers()
                .lastHeader(CustomerSyncKafkaErrorConfiguration.FAILURE_CATEGORY_HEADER).value(),
                StandardCharsets.UTF_8)).isEqualTo("RETRY_EXHAUSTED");
    }
}
