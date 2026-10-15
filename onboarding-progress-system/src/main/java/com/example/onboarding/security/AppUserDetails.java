package com.example.onboarding.security;

import com.example.onboarding.entity.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * The logged-in user. Keeps the user id and name in addition to what Spring Security needs.
 */
public class AppUserDetails extends org.springframework.security.core.userdetails.User {

    private final Long userId;
    private final String displayName;

    public AppUserDetails(User user) {
        super(user.getEmail(), user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        this.userId = user.getId();
        this.displayName = user.getName();
    }

    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }
}
