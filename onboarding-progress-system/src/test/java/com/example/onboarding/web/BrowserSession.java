package com.example.onboarding.web;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Talks to the running app like a browser: keeps cookies (the login session) and sends the
 * CSRF token with forms. Used by tests that need the real server (e.g. error pages, login).
 */
class BrowserSession {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    private final String baseUrl;
    private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    BrowserSession(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    HttpResponse<String> get(String path) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path)).header("Accept", "text/html").GET().build());
    }

    /** Logs in through the login page. Returns the response of POST /login (a redirect). */
    HttpResponse<String> login(String email, String password) {
        String csrf = csrfTokenOf(get("/login").body());
        String form = "username=" + encode(email) + "&password=" + encode(password) + "&_csrf=" + encode(csrf);
        return send(HttpRequest.newBuilder(URI.create(baseUrl + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build());
    }

    /** Sends an empty form to the path, with the CSRF token taken from the given page. */
    HttpResponse<String> post(String formPage, String path) {
        String csrf = csrfTokenOf(get(formPage).body());
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("_csrf=" + encode(csrf)))
                .build());
    }

    private static String csrfTokenOf(String html) {
        Matcher matcher = CSRF.matcher(html);
        if (!matcher.find()) {
            throw new IllegalStateException("No CSRF token on the page");
        }
        return matcher.group(1);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
