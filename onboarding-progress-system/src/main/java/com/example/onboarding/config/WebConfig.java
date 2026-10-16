package com.example.onboarding.config;

import com.example.onboarding.web.PasswordChangeRequiredInterceptor;
import com.example.onboarding.web.RemovedUserLogoutInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RemovedUserLogoutInterceptor removedUserLogoutInterceptor;
    private final PasswordChangeRequiredInterceptor passwordChangeRequiredInterceptor;

    public WebConfig(RemovedUserLogoutInterceptor removedUserLogoutInterceptor,
                     PasswordChangeRequiredInterceptor passwordChangeRequiredInterceptor) {
        this.removedUserLogoutInterceptor = removedUserLogoutInterceptor;
        this.passwordChangeRequiredInterceptor = passwordChangeRequiredInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Runs first: a deleted user, or one whose role changed, is logged out on any page
        registry.addInterceptor(removedUserLogoutInterceptor)
                .excludePathPatterns("/login", "/join/**", "/css/**", "/error", "/favicon.ico");
        // Pages that must stay open while a password change is required
        registry.addInterceptor(passwordChangeRequiredInterceptor)
                .excludePathPatterns("/me/password", "/login", "/join/**", "/css/**", "/error", "/favicon.ico");
    }
}
