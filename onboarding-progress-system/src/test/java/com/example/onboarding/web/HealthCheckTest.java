package com.example.onboarding.web;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Health check (/actuator/health): open to everyone, says only UP or DOWN.
 * Other actuator endpoints (settings, environment variables, ...) are not published.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
class HealthCheckTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsOpenWithoutLoginAndShowsOnlyTheStatus() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", true));
    }

    @Test
    void otherEndpointsNeedLogin() throws Exception {
        for (String path : new String[] {"/actuator/env", "/actuator/configprops", "/actuator/beans"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void otherEndpointsAreNotPublishedEvenForAdministrators() throws Exception {
        for (String path : new String[] {"/actuator/env", "/actuator/configprops", "/actuator/beans", "/actuator/health/db"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isNotFound());
        }
    }
}
