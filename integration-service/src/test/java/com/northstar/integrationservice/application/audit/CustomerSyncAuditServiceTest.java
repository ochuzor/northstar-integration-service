package com.northstar.integrationservice.application.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.northstar.integrationservice.domain.audit.CustomerSyncAuditFailureCategory;
import com.northstar.integrationservice.domain.audit.CustomerSyncAuditStatus;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPayload;
import com.northstar.integrationservice.messaging.customer.CustomerSyncRequestedEvent;
import com.northstar.integrationservice.persistence.audit.CustomerSyncAuditEntity;
import com.northstar.integrationservice.persistence.audit.CustomerSyncAuditRepository;

@ExtendWith(MockitoExtension.class)
class CustomerSyncAuditServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID
            .fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-09-05T10:15:30Z");

    @Mock
    private CustomerSyncAuditRepository repository;

    private CustomerSyncAuditService service;

    @BeforeEach
    void setUp() {
        service = new CustomerSyncAuditService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordsInitiatedAuditAtCurrentTime() {
        CustomerSyncRequestedEvent event = event();

        service.recordInitiated(event);

        ArgumentCaptor<CustomerSyncAuditEntity> auditCaptor = ArgumentCaptor
                .forClass(CustomerSyncAuditEntity.class);
        verify(repository).save(auditCaptor.capture());
        CustomerSyncAuditEntity audit = auditCaptor.getValue();
        assertThat(audit.getCorrelationId()).isEqualTo(CORRELATION_ID);
        assertThat(audit.getEventId()).isEqualTo(EVENT_ID);
        assertThat(audit.getSourceCustomerId()).isEqualTo("001ABC123456789");
        assertThat(audit.getStatus()).isEqualTo(CustomerSyncAuditStatus.INITIATED);
        assertThat(audit.getCreatedAt()).isEqualTo(NOW);
        assertThat(audit.getUpdatedAt()).isEqualTo(NOW);
        assertThat(audit.getFailureCategory()).isNull();
    }

    @Test
    void marksAuditPublished() {
        CustomerSyncAuditEntity audit = initiatedAudit();
        when(repository.findById(CORRELATION_ID)).thenReturn(java.util.Optional.of(audit));

        service.markPublished(CORRELATION_ID);

        assertThat(audit.getStatus()).isEqualTo(CustomerSyncAuditStatus.PUBLISHED);
        assertThat(audit.getUpdatedAt()).isEqualTo(NOW);
        assertThat(audit.getFailureCategory()).isNull();
    }

    @Test
    void marksAuditPublicationFailedWithSafeCategory() {
        CustomerSyncAuditEntity audit = initiatedAudit();
        when(repository.findById(CORRELATION_ID)).thenReturn(java.util.Optional.of(audit));

        service.markPublicationFailed(CORRELATION_ID);

        assertThat(audit.getStatus()).isEqualTo(CustomerSyncAuditStatus.PUBLICATION_FAILED);
        assertThat(audit.getUpdatedAt()).isEqualTo(NOW);
        assertThat(audit.getFailureCategory())
                .isEqualTo(CustomerSyncAuditFailureCategory.KAFKA_PUBLICATION);
    }

    @Test
    void returnsAuditByCorrelationId() {
        CustomerSyncAuditEntity audit = new CustomerSyncAuditEntity(CORRELATION_ID, EVENT_ID,
                "001ABC123456789", CustomerSyncAuditStatus.PUBLICATION_FAILED, NOW.minusSeconds(2),
                NOW, CustomerSyncAuditFailureCategory.KAFKA_PUBLICATION);
        when(repository.findById(CORRELATION_ID)).thenReturn(Optional.of(audit));

        CustomerSyncAuditResult result = service.findByCorrelationId(CORRELATION_ID);

        assertThat(result).isEqualTo(new CustomerSyncAuditResult(CORRELATION_ID, EVENT_ID,
                "001ABC123456789", CustomerSyncAuditStatus.PUBLICATION_FAILED, NOW.minusSeconds(2),
                NOW, CustomerSyncAuditFailureCategory.KAFKA_PUBLICATION));
    }

    @Test
    void throwsAuditNotFoundWhenCorrelationIdDoesNotExist() {
        when(repository.findById(CORRELATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCorrelationId(CORRELATION_ID))
                .isInstanceOf(CustomerSyncAuditNotFoundException.class)
                .hasMessage("Audit not found: " + CORRELATION_ID);
    }

    private CustomerSyncAuditEntity initiatedAudit() {
        return new CustomerSyncAuditEntity(CORRELATION_ID, EVENT_ID, "001ABC123456789",
                CustomerSyncAuditStatus.INITIATED, NOW.minusSeconds(1), NOW.minusSeconds(1), null);
    }

    private CustomerSyncRequestedEvent event() {
        return new CustomerSyncRequestedEvent(EVENT_ID, CORRELATION_ID, NOW, 1,
                "CUSTOMER_SYNC_REQUESTED", "SALESFORCE", new CustomerSyncPayload("001ABC123456789",
                        "NORTHSTAR-001", "Designated Test Account", "Helsinki"));
    }
}
