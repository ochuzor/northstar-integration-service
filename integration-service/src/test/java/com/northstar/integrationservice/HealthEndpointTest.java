package com.northstar.integrationservice;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "salesforce.oauth.token-url=https://auth.example.test/services/oauth2/token",
        "salesforce.oauth.client-id=test-client", "salesforce.oauth.client-secret=test-secret",
        "salesforce.api.version=v66.0"})
@AutoConfigureMockMvc
@Import(IntegrationPostgreSqlTestConfiguration.class)
class HealthEndpointTest {

    private final MockMvc mockMvc;

    @Autowired
    HealthEndpointTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void exposesLivenessWithoutComponentDetails() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk())
                .andExpect(jsonPath("$.*").value(hasSize(1)))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void exposesReadinessWithoutComponentDetails() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk())
                .andExpect(jsonPath("$.*").value(hasSize(1)))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void doesNotExposeUnapprovedActuatorEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }
}
