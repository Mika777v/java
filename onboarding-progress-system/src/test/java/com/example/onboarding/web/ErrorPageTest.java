package com.example.onboarding.web;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import com.example.onboarding.entity.User;
import com.example.onboarding.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Error pages are rendered by Spring Boot's error handling inside the real web server,
 * so these tests start the app on a random port and send real HTTP requests
 * (MockMvc does not go through the error page mechanism).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.admin.email=" + ErrorPageTest.ADMIN_EMAIL, "app.admin.password=" + ErrorPageTest.ADMIN_PASSWORD})
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class, ErrorPageTest.FailingPageConfiguration.class})
class ErrorPageTest {

    static final String ADMIN_EMAIL = "admin@example.com";
    static final String ADMIN_PASSWORD = "admin-password-for-tests";

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private BrowserSession admin;

    @BeforeEach
    void logInAsAdmin() {
        admin = new BrowserSession(port);
        admin.login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    /** A page that always fails, to check the 5xx error page. Only exists in this test. */
    @TestConfiguration(proxyBeanMethods = false)
    static class FailingPageConfiguration {

        @Bean
        FailingPageController failingPageController() {
            return new FailingPageController();
        }
    }

    @Controller
    static class FailingPageController {

        @GetMapping("/test/failing-page")
        String fail() {
            throw new IllegalStateException("Simulated failure with secret details");
        }
    }


    @Test
    void unknownUrlShowsJapaneseNotFoundPage() {
        HttpResponse<String> response = admin.get("/no-such-page");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body())
                .contains("ページが見つかりません")
                .contains("エラー 404")
                .contains("URL: /no-such-page")
                .contains("タスク一覧へ")
                .doesNotContain("Whitelabel Error Page");
    }

    @Test
    void missingTaskShowsJapaneseNotFoundPage() {
        HttpResponse<String> response = admin.get("/tasks/9999/edit");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("ページが見つかりません");
    }

    @Test
    void badRequestShowsJapanese4xxPage() {
        HttpResponse<String> response = admin.get("/users?status=SOMETHING");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body())
                .contains("リクエストを処理できません")
                .contains("エラー 400");
    }

    @Test
    void serverErrorShowsJapanese5xxPageWithoutDetails() {
        HttpResponse<String> response = admin.get("/test/failing-page");

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body())
                .contains("エラーが発生しました")
                .contains("エラー 500")
                .doesNotContain("Simulated failure")
                .doesNotContain("IllegalStateException");
    }

    @Test
    void newEmployeeGetsJapaneseForbiddenPage() {
        User sato = userRepository.findById(2L).orElseThrow();
        sato.setPasswordHash(passwordEncoder.encode("sato-password"));
        userRepository.save(sato);
        BrowserSession member = new BrowserSession(port);

        HttpResponse<String> login = member.login("sato@example.com", "sato-password");
        assertThat(login.headers().firstValue("Location")).hasValueSatisfying(url -> assertThat(url).endsWith("/"));
        assertThat(member.get("/").headers().firstValue("Location"))
                .hasValueSatisfying(url -> assertThat(url).endsWith("/me"));
        assertThat(member.get("/me").body()).contains("佐藤 花子 さんのタスク");

        HttpResponse<String> forbidden = member.get("/tasks");
        assertThat(forbidden.statusCode()).isEqualTo(403);
        assertThat(forbidden.body())
                .contains("このページを表示する権限がありません")
                .contains("エラー 403")
                .contains("マイページへ")
                .doesNotContain("タスク一覧へ");
    }

    @Test
    void apiStillReturnsJsonErrors() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<String> response = restTemplate.withBasicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)
                .exchange("/api/tasks/9999", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(response.getBody()).contains("\"status\":404").doesNotContain("<html");
    }
}
