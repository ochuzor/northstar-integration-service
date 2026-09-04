package com.northstar.mockerp.messaging.config;

import static org.springframework.kafka.listener.DeadLetterPublishingRecoverer.HeaderNames.HeadersToAdd.EX_MSG;
import static org.springframework.kafka.listener.DeadLetterPublishingRecoverer.HeaderNames.HeadersToAdd.EX_STACKTRACE;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.northstar.mockerp.domain.customer.ErpCustomerValidationException;

@Configuration(proxyBeanMethods = false)
public class CustomerSyncKafkaErrorConfiguration {

    public static final String FAILURE_CATEGORY_HEADER = "northstar-failure-category";

    @Bean
    FixedBackOff customerSyncRetryBackOff(CustomerSyncConsumerProperties properties) {
        return new FixedBackOff(properties.retryBackoff().toMillis(),
                properties.maxAttempts() - 1L);
    }

    @Bean
    DeadLetterPublishingRecoverer customerSyncDeadLetterRecoverer(
            KafkaTemplate<Object, Object> kafkaTemplate,
            CustomerSyncConsumerProperties properties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(
                        record.topic() + properties.deadLetterSuffix(), record.partition()));
        recoverer.excludeHeader(EX_MSG, EX_STACKTRACE);
        recoverer.addHeadersFunction(
                (record, exception) -> new RecordHeaders().add(FAILURE_CATEGORY_HEADER,
                        failureCategory(exception).getBytes(StandardCharsets.UTF_8)));
        return recoverer;
    }

    @Bean
    DefaultErrorHandler customerSyncErrorHandler(DeadLetterPublishingRecoverer recoverer,
            FixedBackOff customerSyncRetryBackOff) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
                customerSyncRetryBackOff);
        errorHandler.addNotRetryableExceptions(ErpCustomerValidationException.class);
        return errorHandler;
    }

    private String failureCategory(Exception exception) {
        Throwable failure = exception;
        while (failure != null) {
            if (failure instanceof ErpCustomerValidationException) {
                return "VALIDATION";
            }
            failure = failure.getCause();
        }
        return "RETRY_EXHAUSTED";
    }
}
