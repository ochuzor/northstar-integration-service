package com.northstar.integrationservice;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.northstar.integrationservice.domain.audit.CustomerSyncAuditStatus;
import com.northstar.integrationservice.persistence.audit.CustomerSyncAuditEntity;
import com.northstar.integrationservice.persistence.audit.CustomerSyncAuditRepository;

@SpringBootTest(properties = {
        "salesforce.oauth.token-url=https://auth.example.test/services/oauth2/token",
        "salesforce.oauth.client-id=test-client", "salesforce.oauth.client-secret=test-secret",
        "salesforce.api.version=v66.0"})
@AutoConfigureMockMvc
@Import(IntegrationPostgreSqlTestConfiguration.class)
class CustomerSyncAuditQueryIntegrationTest {

    private static final UUID CORRELATION_ID = UUID
            .fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final MockMvc mockMvc;
    private final CustomerSyncAuditRepository auditRepository;

    @Autowired
    CustomerSyncAuditQueryIntegrationTest(MockMvc mockMvc,
            CustomerSyncAuditRepository auditRepository) {
        this.mockMvc = mockMvc;
        this.auditRepository = auditRepository;
    }

    @BeforeEach
    void clearAudits() {
        auditRepository.deleteAll();
    }

    @Test
    void returnsPersistedAuditByCorrelationId() throws Exception {
        auditRepository.saveAndFlush(
                new CustomerSyncAuditEntity(CORRELATION_ID, EVENT_ID, "001ABC123456789",
                        CustomerSyncAuditStatus.PUBLISHED, Instant.parse("2026-09-05T10:15:30Z"),
                        Instant.parse("2026-09-05T10:15:31Z"), null));

        mockMvc.perform(get("/api/sync/{correlationId}", CORRELATION_ID)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.*").value(hasSize(7)))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID.toString()))
                .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.salesforceAccountId").value("001ABC123456789"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-05T10:15:30Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-09-05T10:15:31Z"))
                .andExpect(jsonPath("$.failureCategory").value(nullValue()));
    }
}
