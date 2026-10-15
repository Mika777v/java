package com.example.onboarding.web;

import com.example.onboarding.security.AppUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes the name of the logged-in user available to every page as ${currentUserName} (for the menu).
 */
@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute("currentUserName")
    public String currentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof AppUserDetails user) {
            return user.getDisplayName();
        }
        return "anonymousUser".equals(authentication.getName()) ? null : authentication.getName();
    }
}
