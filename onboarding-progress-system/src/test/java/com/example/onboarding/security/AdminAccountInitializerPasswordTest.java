package com.example.onboarding.security;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import com.example.onboarding.entity.Role;
import com.example.onboarding.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * APP_ADMIN_PASSWORD must follow the same rule as other passwords (8-72 characters).
 * A password outside the rule is not used: the app still starts, and the log says what to fix.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
class AdminAccountInitializerPasswordTest {

    private static final String EMAIL = "new-admin@example.com";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void removeAdministrators() {
        userRepository.findAll().stream().filter(user -> user.getRole() == Role.ADMIN).forEach(userRepository::delete);
    }

    private void start(String password, boolean reset) {
        new AdminAccountInitializer(userRepository, passwordEncoder, EMAIL, password, "管理者", reset).run(null);
    }

    @Test
    void createsAdministratorWithValidPassword() {
        start("a".repeat(8), false);

        assertThat(userRepository.existsByRole(Role.ADMIN)).isTrue();
    }

    @Test
    void doesNotCreateAdministratorWithTooShortPassword() {
        start("short", false);

        assertThat(userRepository.existsByRole(Role.ADMIN)).isFalse();
    }

    @Test
    void doesNotCreateAdministratorWithTooLongPasswordAndStillStarts() {
        start("a".repeat(73), false);

        assertThat(userRepository.existsByRole(Role.ADMIN)).isFalse();
    }
}
