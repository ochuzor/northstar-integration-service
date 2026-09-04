package com.northstar.integrationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.northstar.integrationservice.application.customer.CustomerPreparationService;
import com.northstar.integrationservice.application.customer.CustomerSynchronizationService;
import com.northstar.integrationservice.domain.audit.CustomerSyncAuditFailureCategory;
import com.northstar.integrationservice.domain.audit.CustomerSyncAuditStatus;
import com.northstar.integrationservice.domain.customer.Customer;
import com.northstar.integrationservice.messaging.customer.CustomerSyncEventFactory;
import com.northstar.integrationservice.messaging.customer.CustomerSyncEventPublisher;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPayload;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationException;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationResult;
import com.northstar.integrationservice.messaging.customer.CustomerSyncRequestedEvent;
import com.northstar.integrationservice.persistence.audit.CustomerSyncAuditRepository;

@SpringBootTest(properties = {
        "salesforce.oauth.token-url=https://auth.example.test/services/oauth2/token",
        "salesforce.oauth.client-id=test-client", "salesforce.oauth.client-secret=test-secret",
        "salesforce.api.version=v66.0"})
@Import(IntegrationPostgreSqlTestConfiguration.class)
class CustomerSynchronizationAuditIntegrationTest {

    private static final String SOURCE_CUSTOMER_ID = "001ABC123456789";
    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID
            .fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-09-05T10:15:30Z");

    private final CustomerSynchronizationService synchronizationService;
    private final CustomerSyncAuditRepository auditRepository;

    @MockitoBean
    private CustomerPreparationService customerPreparationService;

    @MockitoBean
    private CustomerSyncEventFactory eventFactory;

    @MockitoBean
    private CustomerSyncEventPublisher eventPublisher;

    @MockitoBean
    private Clock clock;

    @Autowired
    CustomerSynchronizationAuditIntegrationTest(
            CustomerSynchronizationService synchronizationService,
            CustomerSyncAuditRepository auditRepository) {
        this.synchronizationService = synchronizationService;
        this.auditRepository = auditRepository;
    }

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
        when(clock.instant()).thenReturn(NOW);
    }

    @Test
    void persistsPublishedAuditAfterBrokerAcknowledgement() {
        CustomerSyncRequestedEvent event = configurePreparedEvent();
        CustomerSyncPublicationResult publicationResult = new CustomerSyncPublicationResult(
                EVENT_ID, CORRELATION_ID, SOURCE_CUSTOMER_ID);
        when(eventPublisher.publishEvent(event)).thenReturn(publicationResult);

        CustomerSyncPublicationResult result = synchronizationService
                .synchronizeAccount(SOURCE_CUSTOMER_ID);

        assertThat(result).isSameAs(publicationResult);
        assertThat(auditRepository.findById(CORRELATION_ID)).hasValueSatisfying(audit -> {
            assertThat(audit.getEventId()).isEqualTo(EVENT_ID);
            assertThat(audit.getSourceCustomerId()).isEqualTo(SOURCE_CUSTOMER_ID);
            assertThat(audit.getStatus()).isEqualTo(CustomerSyncAuditStatus.PUBLISHED);
            assertThat(audit.getCreatedAt()).isEqualTo(NOW);
            assertThat(audit.getUpdatedAt()).isEqualTo(NOW);
            assertThat(audit.getFailureCategory()).isNull();
        });
    }

    @Test
    void persistsPublicationFailureAndRethrowsSanitizedException() {
        CustomerSyncRequestedEvent event = configurePreparedEvent();
        CustomerSyncPublicationException publicationFailure = new CustomerSyncPublicationException();
        when(eventPublisher.publishEvent(event)).thenThrow(publicationFailure);

        assertThatThrownBy(() -> synchronizationService.synchronizeAccount(SOURCE_CUSTOMER_ID))
                .isSameAs(publicationFailure).hasMessage("Customer sync event publication failed");

        assertThat(auditRepository.findById(CORRELATION_ID)).hasValueSatisfying(audit -> {
            assertThat(audit.getEventId()).isEqualTo(EVENT_ID);
            assertThat(audit.getStatus()).isEqualTo(CustomerSyncAuditStatus.PUBLICATION_FAILED);
            assertThat(audit.getFailureCategory())
                    .isEqualTo(CustomerSyncAuditFailureCategory.KAFKA_PUBLICATION);
        });
    }

    private CustomerSyncRequestedEvent configurePreparedEvent() {
        Customer customer = new Customer(SOURCE_CUSTOMER_ID, "NORTHSTAR-001",
                "Designated Test Account", "Helsinki");
        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(EVENT_ID, CORRELATION_ID,
                NOW, 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                new CustomerSyncPayload(SOURCE_CUSTOMER_ID, "NORTHSTAR-001",
                        "Designated Test Account", "Helsinki"));
        when(customerPreparationService.prepareCustomer(SOURCE_CUSTOMER_ID)).thenReturn(customer);
        when(eventFactory.generateEvent(customer)).thenReturn(event);
        return event;
    }
}
