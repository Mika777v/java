package com.example.onboarding.security;

import com.example.onboarding.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks up the user who is logging in by email address.
 * Users without a password cannot log in.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmailIgnoreCase(email == null ? "" : email.strip())
                .filter(user -> user.isLoginEnabled())
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found or cannot log in"));
    }
}
