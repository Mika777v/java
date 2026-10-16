package com.example.onboarding.web;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import com.example.onboarding.entity.Role;
import com.example.onboarding.entity.User;
import com.example.onboarding.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An administrator who is deleted, or whose role is changed, must lose access at once,
 * even while logged in (the session still holds the old role).
 * Uses the real server, because the login session is kept in a cookie.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.admin.email=first-admin@example.com",
                "app.admin.password=" + RemovedAdministratorSessionTest.PASSWORD})
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
class RemovedAdministratorSessionTest {

    static final String PASSWORD = "password-for-session-tests";
    private static final String EMAIL = "second-admin@example.com";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User secondAdmin;
    private BrowserSession browser;

    @BeforeEach
    void logInAsSecondAdministrator() {
        User user = new User();
        user.setName("管理者B");
        user.setEmail(EMAIL);
        user.setRole(Role.ADMIN);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        secondAdmin = userRepository.save(user);

        browser = new BrowserSession(port);
        assertThat(browser.login(EMAIL, PASSWORD).headers().firstValue("Location")).hasValue("/");
        assertThat(browser.get("/users").statusCode()).isEqualTo(200);
    }

    @AfterEach
    void removeSecondAdministrator() {
        userRepository.findByEmailIgnoreCase(EMAIL).ifPresent(userRepository::delete);
    }

    private static void assertSentToLogin(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location")).hasValue("/login");
    }

    @Test
    void deletedAdministratorIsLoggedOut() {
        userRepository.delete(secondAdmin);

        assertSentToLogin(browser.get("/users"));
        // The session is gone: the next page also asks for a login
        assertSentToLogin(browser.get("/"));
    }

    @Test
    void administratorChangedToNewEmployeeIsLoggedOut() {
        secondAdmin.setRole(Role.MEMBER);
        userRepository.save(secondAdmin);

        assertSentToLogin(browser.get("/users"));
        assertSentToLogin(browser.get("/me"));
    }

    @Test
    void administratorWhoseRoleIsUnchangedKeepsWorking() {
        secondAdmin.setDepartment("人事部");
        userRepository.save(secondAdmin);

        assertThat(browser.get("/users").statusCode()).isEqualTo(200);
    }
}
