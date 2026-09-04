package com.northstar.integrationservice.persistence.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.northstar.integrationservice.IntegrationPostgreSqlTestConfiguration;
import com.northstar.integrationservice.domain.audit.CustomerSyncAuditStatus;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(IntegrationPostgreSqlTestConfiguration.class)
class CustomerSyncAuditRepositoryTest {

    private static final UUID CORRELATION_ID = UUID
            .fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EVENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String SOURCE_CUSTOMER_ID = "001ABC123456789012";

    private final CustomerSyncAuditRepository repository;
    private final EntityManager entityManager;

    @Autowired
    CustomerSyncAuditRepositoryTest(CustomerSyncAuditRepository repository,
            EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Test
    void savesAndFindsCustomerSyncAuditByCorrelationId() {
        Instant createdAt = Instant.parse("2026-09-05T08:00:00Z");
        CustomerSyncAuditEntity audit = new CustomerSyncAuditEntity(CORRELATION_ID, EVENT_ID,
                SOURCE_CUSTOMER_ID, CustomerSyncAuditStatus.INITIATED, createdAt, createdAt, null);

        repository.saveAndFlush(audit);
        entityManager.clear();

        assertThat(repository.findById(CORRELATION_ID)).hasValueSatisfying(savedAudit -> {
            assertThat(savedAudit.getEventId()).isEqualTo(EVENT_ID);
            assertThat(savedAudit.getSourceCustomerId()).isEqualTo(SOURCE_CUSTOMER_ID);
            assertThat(savedAudit.getStatus()).isEqualTo(CustomerSyncAuditStatus.INITIATED);
            assertThat(savedAudit.getCreatedAt()).isEqualTo(createdAt);
            assertThat(savedAudit.getUpdatedAt()).isEqualTo(createdAt);
            assertThat(savedAudit.getFailureCategory()).isNull();
        });
    }

    @Test
    void findsCustomerSyncAuditByEventId() {
        Instant createdAt = Instant.parse("2026-09-05T08:00:00Z");
        repository.saveAndFlush(new CustomerSyncAuditEntity(CORRELATION_ID, EVENT_ID,
                SOURCE_CUSTOMER_ID, CustomerSyncAuditStatus.PUBLISHED, createdAt, createdAt, null));
        entityManager.clear();

        assertThat(repository.findByEventId(EVENT_ID)).hasValueSatisfying(
                audit -> assertThat(audit.getCorrelationId()).isEqualTo(CORRELATION_ID));
    }
}
