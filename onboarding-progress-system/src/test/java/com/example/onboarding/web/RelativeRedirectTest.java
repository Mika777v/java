package com.example.onboarding.web;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redirects must say only the path ("/users/3"), not "http://localhost:8080/users/3".
 * Behind a proxy such as GitHub Codespaces the app is reached as localhost:8080,
 * and a full address would send the browser to localhost ("connection refused").
 * Uses the real server, because the redirect address is decided by the web server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.admin.email=" + RelativeRedirectTest.ADMIN_EMAIL,
                "app.admin.password=" + RelativeRedirectTest.ADMIN_PASSWORD})
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
class RelativeRedirectTest {

    static final String ADMIN_EMAIL = "admin@example.com";
    static final String ADMIN_PASSWORD = "admin-password-for-redirect-tests";

    @LocalServerPort
    private int port;

    private static String locationOf(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(302);
        return response.headers().firstValue("Location").orElseThrow();
    }

    @Test
    void loginRedirectsUsePathsOnly() {
        BrowserSession browser = new BrowserSession(port);

        assertThat(locationOf(browser.get("/users"))).isEqualTo("/login");
        assertThat(locationOf(browser.login(ADMIN_EMAIL, "wrong-password"))).isEqualTo("/login?error");
        // Back to the page that was asked for before logging in
        assertThat(locationOf(browser.login(ADMIN_EMAIL, ADMIN_PASSWORD))).isEqualTo("/users?continue");
        assertThat(locationOf(browser.post("/users", "/logout"))).isEqualTo("/login?logout");
    }

    @Test
    void redirectsAfterFormsUsePathsOnly() {
        BrowserSession browser = new BrowserSession(port);
        assertThat(locationOf(browser.login(ADMIN_EMAIL, ADMIN_PASSWORD))).isEqualTo("/");

        // The case found in Codespaces: issuing a personal link
        assertThat(locationOf(browser.post("/users/3", "/users/3/access-link"))).isEqualTo("/users/3");
        assertThat(locationOf(browser.post("/users/1", "/users/1/assignments/all"))).isEqualTo("/users/1");
    }
}
