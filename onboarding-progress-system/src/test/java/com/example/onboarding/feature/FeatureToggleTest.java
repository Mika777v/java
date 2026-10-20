package com.example.onboarding.feature;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pages marked with @RequiresFeature answer 404 while the feature is off.
 * Uses a controller that exists only in this test (nested classes of tests are not picked up elsewhere).
 */
@SpringBootTest(properties = "features.kanban-board.enabled=true")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class, FeatureToggleTest.ToggledController.class})
@Transactional
@WithMockUser(roles = "ADMIN")
class FeatureToggleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @RestController
    @RequiresFeature(Feature.TICKET_SYSTEM)
    static class ToggledController {

        @GetMapping("/feature-test/ticket")
        String ticket() {
            return "ticket";
        }

        @GetMapping("/feature-test/kanban")
        @RequiresFeature(Feature.KANBAN_BOARD)
        String kanban() {
            return "kanban";
        }

        @GetMapping("/feature-test/legacy")
        @RequiresFeature(Feature.ONBOARDING_LEGACY)
        String legacy() {
            return "legacy";
        }
    }

    @Test
    void pageOfFeatureThatIsOffIsNotFound() throws Exception {
        mockMvc.perform(get("/feature-test/ticket")).andExpect(status().isNotFound());
    }

    @Test
    void pageOfChildFeatureIsNotFoundWhileParentIsOff() throws Exception {
        mockMvc.perform(get("/feature-test/kanban")).andExpect(status().isNotFound());
    }

    @Test
    void methodAnnotationWinsOverClassAnnotation() throws Exception {
        mockMvc.perform(get("/feature-test/legacy"))
                .andExpect(status().isOk())
                .andExpect(content().string("legacy"));
    }

    @Test
    void existingPagesAreUnchanged() throws Exception {
        mockMvc.perform(get("/tasks")).andExpect(status().isOk());
    }

    @Test
    void templatesCanUseFeaturesBean() {
        assertThat(context.getBean("features")).isInstanceOf(FeatureFlags.class);
    }
}
