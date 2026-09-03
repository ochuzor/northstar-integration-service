package com.northstar.mockerp.persistence.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.northstar.mockerp.PostgreSqlTestConfiguration;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(PostgreSqlTestConfiguration.class)
class ErpCustomerRepositoryTest {

    private final ErpCustomerRepository repository;
    private final EntityManager entityManager;

    @Autowired
    ErpCustomerRepositoryTest(ErpCustomerRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Test
    void savesAndFindsCustomerBySourceCustomerId() {
        ErpCustomerEntity customer = new ErpCustomerEntity("001ABC123456789012", "NORTHSTAR-001",
                "Designated Test Account", "Helsinki");

        repository.saveAndFlush(customer);
        entityManager.clear();

        assertThat(repository.findBySourceCustomerId("001ABC123456789012"))
                .hasValueSatisfying(savedCustomer -> {
                    assertThat(savedCustomer.getId()).isNotNull();
                    assertThat(savedCustomer.getBusinessId()).isEqualTo("NORTHSTAR-001");
                    assertThat(savedCustomer.getName()).isEqualTo("Designated Test Account");
                    assertThat(savedCustomer.getBillingCity()).isEqualTo("Helsinki");
                });
    }

    @Test
    void persistsCustomerWithoutBillingCity() {
        ErpCustomerEntity customer = new ErpCustomerEntity("001ABC123456789013", "NORTHSTAR-002",
                "Customer Without City", null);

        repository.saveAndFlush(customer);
        entityManager.clear();

        assertThat(repository.findBySourceCustomerId("001ABC123456789013")).hasValueSatisfying(
                savedCustomer -> assertThat(savedCustomer.getBillingCity()).isNull());
    }

    @Test
    void rejectsDuplicateSourceCustomerId() {
        repository.saveAndFlush(new ErpCustomerEntity("001ABC123456789014", "NORTHSTAR-003",
                "Original Customer", "Helsinki"));

        ErpCustomerEntity duplicate = new ErpCustomerEntity("001ABC123456789014", "NORTHSTAR-004",
                "Duplicate Customer", "Espoo");

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
