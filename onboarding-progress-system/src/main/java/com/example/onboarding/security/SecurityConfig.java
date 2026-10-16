package com.example.onboarding.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.net.URI;

/**
 * Who may open which page.
 *
 * <ul>
 *   <li>REST API (/api/**): administrators only, with HTTP Basic (e.g. curl -u email:password)</li>
 *   <li>Screens: log in on /login (or, for new employees, with a personal link /join/{token}).
 *       "/" and "/me" for every logged-in user (new employees are sent from "/" to "/me");
 *       everything else for administrators only</li>
 *   <li>Health check (/actuator/health): everyone</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
                .httpBasic(Customizer.withDefaults())
                // No browser session for the API, so no CSRF token is needed
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurity(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/join/**", "/css/**", "/error", "/favicon.ico").permitAll()
                        // Health check for Docker, CI and monitoring: only says UP or DOWN
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/", "/me", "/me/**").authenticated()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(loginSuccessHandler())
                        .permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(redirectToLoginPage()))
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .build();
    }

    // Redirects below say only the path (e.g. "/login"), never "http://localhost:8080/login":
    // behind a proxy such as GitHub Codespaces the app is reached as localhost:8080,
    // and a full address would send the browser to localhost.

    /** Not logged in: go to the login page. */
    private static AuthenticationEntryPoint redirectToLoginPage() {
        return (request, response, exception) -> response.sendRedirect(request.getContextPath() + "/login");
    }

    /** After logging in: back to the page that was asked for, otherwise "/". */
    private static SavedRequestAwareAuthenticationSuccessHandler loginSuccessHandler() {
        SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl("/");
        handler.setRedirectStrategy(pathOnlyRedirects());
        return handler;
    }

    /** The page that was asked for is remembered as a full address; only its path and query are used. */
    private static RedirectStrategy pathOnlyRedirects() {
        return (request, response, url) -> {
            URI uri = URI.create(url);
            String target = uri.isAbsolute()
                    ? uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery())
                    : request.getContextPath() + url;
            response.sendRedirect(response.encodeRedirectURL(target));
        };
    }
}
