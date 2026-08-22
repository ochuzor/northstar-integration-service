package com.northstar.integrationservice.application.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.northstar.integrationservice.salesforce.SalesforceSession;
import com.northstar.integrationservice.salesforce.account.SalesforceAccountClient;
import com.northstar.integrationservice.salesforce.account.SalesforceAccountResponse;
import com.northstar.integrationservice.salesforce.oauth.SalesforceAuthenticationUnavailableException;
import com.northstar.integrationservice.salesforce.oauth.SalesforceOAuthClient;

@ExtendWith(MockitoExtension.class)
public class SalesforceAccountServiceTest {
    @Mock
    private SalesforceOAuthClient oauthClient;

    @Mock
    private SalesforceAccountClient accountClient;

    private SalesforceAccountService service;

    @BeforeEach
    void setUp() {
        service = new SalesforceAccountService(oauthClient, accountClient);
    }

    @Test
    void authenticatesAndFetchesAccount() {
        URI instanceUrl = URI.create("https://instance.example.test");
        String accountId = "001ABC123456789";

        SalesforceSession session = new SalesforceSession("synthetic-token", instanceUrl, "Bearer",
                Instant.parse("2026-01-01T00:00:00Z"));
        SalesforceAccountResponse sfAccResp = new SalesforceAccountResponse(accountId,
                "Test Account", "Test Biz Id", "Billing City");

        when(oauthClient.authenticateSession()).thenReturn(session);
        when(accountClient.fetchAccount(session, accountId)).thenReturn(sfAccResp);

        SalesforceAccountResult result = service.fetchAccount(accountId);

        assertThat(result.salesforceAccountId()).isEqualTo(accountId);
        assertThat(result.name()).isEqualTo("Test Account");
        assertThat(result.businessId()).isEqualTo("Test Biz Id");
        assertThat(result.billingCity()).isEqualTo("Billing City");

        verify(oauthClient).authenticateSession();
        verify(accountClient).fetchAccount(session, accountId);
    }

    @Test
    void doesNotFetchAccountWhenAuthenticationFails() {
        String accountId = "001ABC123456789";

        when(oauthClient.authenticateSession())
                .thenThrow(new SalesforceAuthenticationUnavailableException());

        assertThatThrownBy(() -> service.fetchAccount(accountId))
                .isInstanceOf(SalesforceAuthenticationUnavailableException.class)
                .hasMessage("Salesforce authentication server is unavailable");

        verify(oauthClient).authenticateSession();
        verifyNoInteractions(accountClient);
    }
}
