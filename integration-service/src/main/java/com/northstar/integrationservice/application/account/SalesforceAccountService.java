package com.northstar.integrationservice.application.account;

import org.springframework.stereotype.Service;

import com.northstar.integrationservice.salesforce.SalesforceSession;
import com.northstar.integrationservice.salesforce.account.SalesforceAccountClient;
import com.northstar.integrationservice.salesforce.account.SalesforceAccountResponse;
import com.northstar.integrationservice.salesforce.oauth.SalesforceOAuthClient;

@Service
public class SalesforceAccountService {
    private final SalesforceOAuthClient oauthClient;
    private final SalesforceAccountClient accountClient;

    public SalesforceAccountService(SalesforceOAuthClient salesforceOAuthClient,
            SalesforceAccountClient salesforceAccountClient) {
        this.oauthClient = salesforceOAuthClient;
        this.accountClient = salesforceAccountClient;
    }

    public SalesforceAccountResult fetchAccount(String salesforceAccountId) {
        SalesforceSession session = this.oauthClient.authenticateSession();
        SalesforceAccountResponse account = this.accountClient.fetchAccount(session,
                salesforceAccountId);

        return new SalesforceAccountResult(account.id(), account.name(), account.businessId(),
                account.billingCity());
    }
}
