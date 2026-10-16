package com.example.onboarding.security;

import com.example.onboarding.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks up the user who is logging in by email address and password.
 * Users without a password cannot log in. When new employees use personal links
 * (app.member-login=link), only administrators can log in with a password.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoginSettings loginSettings;

    public AppUserDetailsService(UserRepository userRepository, LoginSettings loginSettings) {
        this.userRepository = userRepository;
        this.loginSettings = loginSettings;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmailIgnoreCase(email == null ? "" : email.strip())
                .filter(user -> user.isLoginEnabled())
                .filter(user -> user.isAdmin() || !loginSettings.isLinkMode())
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found or cannot log in"));
    }
}
