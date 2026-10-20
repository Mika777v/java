package com.example.onboarding.feature;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;

/** The interceptor lets requests through when the feature is on (no Spring context needed). */
class FeatureToggleInterceptorTest {

    @RequiresFeature(Feature.TICKET_SYSTEM)
    static class TicketPages {
        public String list() {
            return "list";
        }
    }

    static class PlainPages {
        public String list() {
            return "list";
        }
    }

    private boolean passes(FeatureFlags features, Object controller) throws Exception {
        HandlerMethod handler = new HandlerMethod(controller, controller.getClass().getMethod("list"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean passed = new FeatureToggleInterceptor(features)
                .preHandle(new MockHttpServletRequest(), response, handler);
        assertThat(response.getStatus()).isEqualTo(passed ? 200 : 404);
        return passed;
    }

    @Test
    void passesWhenFeatureIsOn() throws Exception {
        FeatureFlags on = new FeatureFlags(new MockEnvironment()
                .withProperty("features.ticket-system.enabled", "true"));

        assertThat(passes(on, new TicketPages())).isTrue();
    }

    @Test
    void stopsWithNotFoundWhenFeatureIsOff() throws Exception {
        FeatureFlags off = new FeatureFlags(new MockEnvironment());

        assertThat(passes(off, new TicketPages())).isFalse();
    }

    @Test
    void pagesWithoutAnnotationAlwaysPass() throws Exception {
        FeatureFlags off = new FeatureFlags(new MockEnvironment());

        assertThat(passes(off, new PlainPages())).isTrue();
    }
}
