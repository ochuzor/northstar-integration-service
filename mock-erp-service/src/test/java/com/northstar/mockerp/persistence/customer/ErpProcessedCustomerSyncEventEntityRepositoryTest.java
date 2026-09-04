package com.northstar.mockerp.persistence.customer;

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

import com.northstar.mockerp.PostgreSqlTestConfiguration;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(PostgreSqlTestConfiguration.class)
class ErpProcessedCustomerSyncEventEntityRepositoryTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ErpProcessedCustomerSyncEventEntityRepository repository;
    private final EntityManager entityManager;

    @Autowired
    ErpProcessedCustomerSyncEventEntityRepositoryTest(
            ErpProcessedCustomerSyncEventEntityRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Test
    void savesAndFindsProcessedCustomerSyncEvent() {
        Instant processedAt = Instant.parse("2026-09-04T08:00:00Z");
        repository.saveAndFlush(new ErpProcessedCustomerSyncEventEntity(EVENT_ID,
                "001ABC123456789012", processedAt));
        entityManager.clear();

        assertThat(repository.findById(EVENT_ID)).hasValueSatisfying(processedEvent -> {
            assertThat(processedEvent.getSourceCustomerId()).isEqualTo("001ABC123456789012");
            assertThat(processedEvent.getProcessedAt()).isEqualTo(processedAt);
        });
    }
}
