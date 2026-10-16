package com.example.onboarding.web;

import com.example.onboarding.repository.UserRepository;
import com.example.onboarding.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * The login session keeps the role the user had when logging in. When the user is deleted or the
 * role is changed (e.g. an administrator becomes a new employee), the old session must stop working
 * at once, so the user is logged out and sent to the login page.
 */
@Component
public class RemovedUserLogoutInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    public RemovedUserLogoutInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails me)) {
            return true;
        }
        boolean sameUserAndRole = userRepository.findById(me.getUserId())
                .map(user -> me.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + user.getRole().name())))
                .orElse(false);
        if (sameUserAndRole) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
        } else {
            response.sendRedirect(request.getContextPath() + "/login");
        }
        return false;
    }
}
