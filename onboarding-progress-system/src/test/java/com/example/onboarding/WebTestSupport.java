package com.example.onboarding;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * Request builders for tests that include a CSRF token, like a real form sent from the browser.
 * Use these instead of MockMvcRequestBuilders.post / put / delete.
 */
public final class WebTestSupport {

    private WebTestSupport() {
    }

    public static MockHttpServletRequestBuilder post(String url, Object... uriVariables) {
        return MockMvcRequestBuilders.post(url, uriVariables).with(csrf());
    }

    public static MockHttpServletRequestBuilder put(String url, Object... uriVariables) {
        return MockMvcRequestBuilders.put(url, uriVariables).with(csrf());
    }

    public static MockHttpServletRequestBuilder delete(String url, Object... uriVariables) {
        return MockMvcRequestBuilders.delete(url, uriVariables).with(csrf());
    }
}
