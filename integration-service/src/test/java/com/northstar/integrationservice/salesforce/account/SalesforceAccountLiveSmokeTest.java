package com.northstar.integrationservice.salesforce.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.northstar.integrationservice.application.account.SalesforceAccountResult;
import com.northstar.integrationservice.application.account.SalesforceAccountService;

@Tag("live-salesforce")
@SpringBootTest
class SalesforceAccountLiveSmokeTest {

    private final SalesforceAccountService accountService;
    private final String accountId;

    @Autowired
    SalesforceAccountLiveSmokeTest(SalesforceAccountService accountService,
            @Value("${SALESFORCE_TEST_ACCOUNT_ID}") String accountId) {
        this.accountService = accountService;
        this.accountId = accountId;
    }

    @Test
    void fetchesDesignatedAccountFromSalesforce() {
        SalesforceAccountResult result = accountService.fetchAccount(accountId);

        assertThat(result.salesforceAccountId()).isEqualTo(accountId);
        assertThat(result.name()).isNotBlank();
        assertThat(result.businessId()).isNotBlank();
        assertThat(result.billingCity()).isNotBlank();
    }
}
