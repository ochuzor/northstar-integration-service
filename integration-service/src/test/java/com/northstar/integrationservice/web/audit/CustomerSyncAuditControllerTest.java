package com.northstar.integrationservice.web.audit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.northstar.integrationservice.application.audit.CustomerSyncAuditNotFoundException;
import com.northstar.integrationservice.application.audit.CustomerSyncAuditResult;
import com.northstar.integrationservice.application.audit.CustomerSyncAuditService;
import com.northstar.integrationservice.domain.audit.CustomerSyncAuditFailureCategory;
import com.northstar.integrationservice.domain.audit.CustomerSyncAuditStatus;

@WebMvcTest(CustomerSyncAuditController.class)
class CustomerSyncAuditControllerTest {

    private static final UUID CORRELATION_ID = UUID
            .fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerSyncAuditService auditService;

    @Test
    void returnsCustomerSyncAudit() throws Exception {
        when(auditService.findByCorrelationId(CORRELATION_ID))
                .thenReturn(new CustomerSyncAuditResult(CORRELATION_ID, EVENT_ID, "001ABC123456789",
                        CustomerSyncAuditStatus.PUBLICATION_FAILED,
                        Instant.parse("2026-09-05T10:15:30Z"),
                        Instant.parse("2026-09-05T10:15:31Z"),
                        CustomerSyncAuditFailureCategory.KAFKA_PUBLICATION));

        mockMvc.perform(get("/api/sync/{correlationId}", CORRELATION_ID)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID.toString()))
                .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.salesforceAccountId").value("001ABC123456789"))
                .andExpect(jsonPath("$.status").value("PUBLICATION_FAILED"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-05T10:15:30Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-09-05T10:15:31Z"))
                .andExpect(jsonPath("$.failureCategory").value("KAFKA_PUBLICATION"));

        verify(auditService).findByCorrelationId(CORRELATION_ID);
    }

    @Test
    void returnsNotFoundWhenAuditDoesNotExist() throws Exception {
        when(auditService.findByCorrelationId(CORRELATION_ID))
                .thenThrow(new CustomerSyncAuditNotFoundException(CORRELATION_ID));

        mockMvc.perform(get("/api/sync/{correlationId}", CORRELATION_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SYNC_AUDIT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Sync audit was not found"));

        verify(auditService).findByCorrelationId(CORRELATION_ID);
    }
}
