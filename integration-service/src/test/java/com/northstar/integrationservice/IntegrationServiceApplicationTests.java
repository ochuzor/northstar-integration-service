package com.northstar.integrationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
        "salesforce.oauth.token-url=https://auth.example.test/services/oauth2/token",
        "salesforce.oauth.client-id=test-client", "salesforce.oauth.client-secret=test-secret",
        "salesforce.api.version=v66.0"})
@Import(IntegrationPostgreSqlTestConfiguration.class)
class IntegrationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
