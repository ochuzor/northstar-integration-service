package com.northstar.integrationservice.web.account;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.northstar.integrationservice.application.customer.CustomerSynchronizationService;
import com.northstar.integrationservice.domain.customer.CustomerValidationException;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationException;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationResult;
import com.northstar.integrationservice.salesforce.account.SalesforceAccountNotFoundException;
import com.northstar.integrationservice.salesforce.account.SalesforceAccountRequestException;
import com.northstar.integrationservice.salesforce.account.SalesforceAccountUnavailableException;

@WebMvcTest(AccountSyncController.class)
class AccountSyncControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerSynchronizationService customerSynchronizationService;

    @Test
    void returnsSafeAccountForSyncRequest() throws Exception {
        String accountId = "001ABC123456789";
        String eventId = "11111111-1111-1111-1111-111111111111";
        String correlationId = "22222222-2222-2222-2222-222222222222";

        when(customerSynchronizationService.synchronizeAccount(accountId))
                .thenReturn(new CustomerSyncPublicationResult(UUID.fromString(eventId),
                        UUID.fromString(correlationId), accountId));

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.salesforceAccountId").value(accountId))
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.correlationId").value(correlationId))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(customerSynchronizationService).synchronizeAccount(accountId);
    }

    @Test
    void returnsBadRequestForInvalidAccountId() throws Exception {
        String accountId = "invalid-id";

        when(customerSynchronizationService.synchronizeAccount(accountId))
                .thenThrow(new IllegalArgumentException("Internal validation detail"));

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_ACCOUNT_ID"))
                .andExpect(jsonPath("$.message").value("Invalid Salesforce Account ID"));

        verify(customerSynchronizationService).synchronizeAccount(accountId);
    }

    @Test
    void returnsNotFoundWhenAccountDoesNotExist() throws Exception {
        String accountId = "001ABC123456789";

        when(customerSynchronizationService.synchronizeAccount(accountId))
                .thenThrow(new SalesforceAccountNotFoundException(accountId));

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Salesforce Account was not found"));

        verify(customerSynchronizationService).synchronizeAccount(accountId);
    }

    @Test
    void returnsBadGatewayWhenSalesforceRejectsRequest() throws Exception {
        String accountId = "001ABC123456789";

        when(customerSynchronizationService.synchronizeAccount(accountId))
                .thenThrow(new SalesforceAccountRequestException(HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SALESFORCE_UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message").value("Salesforce returned an invalid response"));

        verify(customerSynchronizationService).synchronizeAccount(accountId);
    }

    @Test
    void returnsServiceUnavailableWhenSalesforceThrottlesRequest() throws Exception {
        String accountId = "001ABC123456789";

        when(customerSynchronizationService.synchronizeAccount(accountId))
                .thenThrow(new SalesforceAccountRequestException(HttpStatus.TOO_MANY_REQUESTS));

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SALESFORCE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Salesforce is temporarily unavailable"));

        verify(customerSynchronizationService).synchronizeAccount(accountId);
    }

    @Test
    void returnsServiceUnavailableWhenSalesforceIsUnavailable() throws Exception {
        String accountId = "001ABC123456789";

        when(customerSynchronizationService.synchronizeAccount(accountId))
                .thenThrow(new SalesforceAccountUnavailableException());

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SALESFORCE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Salesforce is temporarily unavailable"));

        verify(customerSynchronizationService).synchronizeAccount(accountId);
    }

    @Test
    void returnsUnprocessableEntityForInvalidCustomer() throws Exception {
        String accountId = "001ABC123456789";

        when(customerSynchronizationService.synchronizeAccount(accountId))
                .thenThrow(new CustomerValidationException(Set.of("businessId")));

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("CUSTOMER_VALIDATION_FAILED")).andExpect(
                        jsonPath("$.message").value("Salesforce Account cannot be synchronized"));

        verify(customerSynchronizationService).synchronizeAccount(accountId);
    }

    @Test
    void returnsServiceUnavailableWhenEventPublicationFails() throws Exception {
        String accountId = "001ABC123456789";

        when(customerSynchronizationService.synchronizeAccount(accountId))
                .thenThrow(new CustomerSyncPublicationException());

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SYNC_PUBLICATION_UNAVAILABLE"))
                .andExpect(jsonPath("$.message")
                        .value("Customer synchronization is temporarily unavailable"));

        verify(customerSynchronizationService).synchronizeAccount(accountId);
    }
}
