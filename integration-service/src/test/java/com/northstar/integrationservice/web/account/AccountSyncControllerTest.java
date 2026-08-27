package com.northstar.integrationservice.web.account;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.northstar.integrationservice.application.customer.CustomerPreparationService;
import com.northstar.integrationservice.domain.customer.Customer;
import com.northstar.integrationservice.domain.customer.CustomerValidationException;
import com.northstar.integrationservice.salesforce.account.SalesforceAccountNotFoundException;
import com.northstar.integrationservice.salesforce.account.SalesforceAccountRequestException;
import com.northstar.integrationservice.salesforce.account.SalesforceAccountUnavailableException;

@WebMvcTest(AccountSyncController.class)
class AccountSyncControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerPreparationService customerPreparationService;

    @Test
    void returnsSafeAccountForSyncRequest() throws Exception {
        String accountId = "001ABC123456789";

        when(customerPreparationService.prepareCustomer(accountId)).thenReturn(
                new Customer(accountId, "NORTHSTAR-001", "Designated Test Account", "Helsinki"));

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesforceAccountId").value(accountId))
                .andExpect(jsonPath("$.name").value("Designated Test Account"))
                .andExpect(jsonPath("$.businessId").value("NORTHSTAR-001"))
                .andExpect(jsonPath("$.billingCity").value("Helsinki"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.instanceUrl").doesNotExist())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(customerPreparationService).prepareCustomer(accountId);
    }

    @Test
    void returnsBadRequestForInvalidAccountId() throws Exception {
        String accountId = "invalid-id";

        when(customerPreparationService.prepareCustomer(accountId))
                .thenThrow(new IllegalArgumentException("Internal validation detail"));

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_ACCOUNT_ID"))
                .andExpect(jsonPath("$.message").value("Invalid Salesforce Account ID"));

        verify(customerPreparationService).prepareCustomer(accountId);
    }

    @Test
    void returnsNotFoundWhenAccountDoesNotExist() throws Exception {
        String accountId = "001ABC123456789";

        when(customerPreparationService.prepareCustomer(accountId))
                .thenThrow(new SalesforceAccountNotFoundException(accountId));

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Salesforce Account was not found"));

        verify(customerPreparationService).prepareCustomer(accountId);
    }

    @Test
    void returnsBadGatewayWhenSalesforceRejectsRequest() throws Exception {
        String accountId = "001ABC123456789";

        when(customerPreparationService.prepareCustomer(accountId))
                .thenThrow(new SalesforceAccountRequestException(HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SALESFORCE_UPSTREAM_ERROR"))
                .andExpect(jsonPath("$.message").value("Salesforce returned an invalid response"));

        verify(customerPreparationService).prepareCustomer(accountId);
    }

    @Test
    void returnsServiceUnavailableWhenSalesforceThrottlesRequest() throws Exception {
        String accountId = "001ABC123456789";

        when(customerPreparationService.prepareCustomer(accountId))
                .thenThrow(new SalesforceAccountRequestException(HttpStatus.TOO_MANY_REQUESTS));

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SALESFORCE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Salesforce is temporarily unavailable"));

        verify(customerPreparationService).prepareCustomer(accountId);
    }

    @Test
    void returnsServiceUnavailableWhenSalesforceIsUnavailable() throws Exception {
        String accountId = "001ABC123456789";

        when(customerPreparationService.prepareCustomer(accountId))
                .thenThrow(new SalesforceAccountUnavailableException());

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SALESFORCE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Salesforce is temporarily unavailable"));

        verify(customerPreparationService).prepareCustomer(accountId);
    }

    @Test
    void returnsUnprocessableEntityForInvalidCustomer() throws Exception {
        String accountId = "001ABC123456789";

        when(customerPreparationService.prepareCustomer(accountId))
                .thenThrow(new CustomerValidationException(Set.of("businessId")));

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("CUSTOMER_VALIDATION_FAILED")).andExpect(
                        jsonPath("$.message").value("Salesforce Account cannot be synchronized"));

        verify(customerPreparationService).prepareCustomer(accountId);
    }
}
