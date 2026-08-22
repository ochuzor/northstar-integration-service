package com.northstar.integrationservice.web.account;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.northstar.integrationservice.application.account.SalesforceAccountResult;
import com.northstar.integrationservice.application.account.SalesforceAccountService;

@WebMvcTest(AccountSyncController.class)
class AccountSyncControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalesforceAccountService accountService;

    @Test
    void returnsSafeAccountForSyncRequest() throws Exception {
        String accountId = "001ABC123456789";

        when(accountService.fetchAccount(accountId)).thenReturn(new SalesforceAccountResult(
                accountId, "Designated Test Account", "NORTHSTAR-001", "Helsinki"));

        mockMvc.perform(post("/api/sync/account/{salesforceAccountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesforceAccountId").value(accountId))
                .andExpect(jsonPath("$.name").value("Designated Test Account"))
                .andExpect(jsonPath("$.businessId").value("NORTHSTAR-001"))
                .andExpect(jsonPath("$.billingCity").value("Helsinki"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.instanceUrl").doesNotExist())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(accountService).fetchAccount(accountId);
    }
}
