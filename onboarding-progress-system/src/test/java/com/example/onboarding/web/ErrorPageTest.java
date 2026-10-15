package com.example.onboarding.web;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Error pages are rendered by Spring Boot's error handling inside the real web server,
 * so these tests start the app on a random port and send real HTTP requests
 * (MockMvc does not go through the error page mechanism).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class, ErrorPageTest.FailingPageConfiguration.class})
class ErrorPageTest {

    @Autowired
    private TestRestTemplate restTemplate;

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

    private ResponseEntity<String> get(String path, MediaType accept) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(accept));
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    @Test
    void unknownUrlShowsJapaneseNotFoundPage() {
        ResponseEntity<String> response = get("/no-such-page", MediaType.TEXT_HTML);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .contains("ページが見つかりません")
                .contains("エラー 404")
                .contains("URL: /no-such-page")
                .contains("タスク一覧へ")
                .doesNotContain("Whitelabel Error Page");
    }

    @Test
    void missingTaskShowsJapaneseNotFoundPage() {
        ResponseEntity<String> response = get("/tasks/9999/edit", MediaType.TEXT_HTML);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("ページが見つかりません");
    }

    @Test
    void badRequestShowsJapanese4xxPage() {
        ResponseEntity<String> response = get("/users?status=SOMETHING", MediaType.TEXT_HTML);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .contains("リクエストを処理できません")
                .contains("エラー 400");
    }

    @Test
    void serverErrorShowsJapanese5xxPageWithoutDetails() {
        ResponseEntity<String> response = get("/test/failing-page", MediaType.TEXT_HTML);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .contains("エラーが発生しました")
                .contains("エラー 500")
                .doesNotContain("Simulated failure")
                .doesNotContain("IllegalStateException");
    }

    @Test
    void apiStillReturnsJsonErrors() {
        ResponseEntity<String> response = get("/api/tasks/9999", MediaType.APPLICATION_JSON);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(response.getBody()).contains("\"status\":404").doesNotContain("<html");
    }
}
