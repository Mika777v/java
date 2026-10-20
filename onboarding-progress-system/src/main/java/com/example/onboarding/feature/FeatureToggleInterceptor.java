package com.example.onboarding.feature;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Answers 404 for controllers marked with {@link RequiresFeature} while the feature is off.
 * 404 rather than 403: a feature that is not released yet should look like it does not exist.
 */
@Component
public class FeatureToggleInterceptor implements HandlerInterceptor {

    private final FeatureFlags features;

    public FeatureToggleInterceptor(FeatureFlags features) {
        this.features = features;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        // A method's own annotation wins over the one on its class
        RequiresFeature required = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequiresFeature.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequiresFeature.class);
        }
        if (required == null || features.isEnabled(required.value())) {
            return true;
        }
        response.sendError(HttpStatus.NOT_FOUND.value());
        return false;
    }
}
