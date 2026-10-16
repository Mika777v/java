package com.example.onboarding.web;

import com.example.onboarding.repository.UserRepository;
import com.example.onboarding.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Users whose password was set by an administrator must change it first:
 * every page except the password change page sends them to /me/password, and the API answers 403.
 * The flag is read from the database on each request, so it takes effect without logging in again.
 */
@Component
public class PasswordChangeRequiredInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    public PasswordChangeRequiredInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails me)) {
            return true;
        }
        boolean required = userRepository.findById(me.getUserId())
                .map(user -> user.isPasswordChangeRequired())
                .orElse(false);
        if (!required) {
            return true;
        }
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Password change required");
        } else {
            response.sendRedirect(request.getContextPath() + "/me/password");
        }
        return false;
    }
}
