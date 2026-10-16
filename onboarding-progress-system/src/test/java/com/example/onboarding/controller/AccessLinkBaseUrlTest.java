package com.example.onboarding.controller;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.example.onboarding.WebTestSupport.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With app.base-url set (e.g. the Codespaces address), personal links use that address
 * instead of the address of the current request.
 */
@SpringBootTest(properties = "app.base-url=https://onboarding.example.com/")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
@WithMockUser(roles = "ADMIN")
class AccessLinkBaseUrlTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void linkUsesConfiguredBaseUrl() throws Exception {
        String link = (String) mockMvc.perform(post("/users/3/access-link"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getFlashMap().get("accessLink");

        assertThat(link).matches("https://onboarding\\.example\\.com/join/[A-Za-z0-9_-]{43}");
    }
}
