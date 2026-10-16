package com.example.onboarding.config;

import com.example.onboarding.web.PasswordChangeRequiredInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PasswordChangeRequiredInterceptor passwordChangeRequiredInterceptor;

    public WebConfig(PasswordChangeRequiredInterceptor passwordChangeRequiredInterceptor) {
        this.passwordChangeRequiredInterceptor = passwordChangeRequiredInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Pages that must stay open while a password change is required
        registry.addInterceptor(passwordChangeRequiredInterceptor)
                .excludePathPatterns("/me/password", "/login", "/join/**", "/css/**", "/error", "/favicon.ico");
    }
}
