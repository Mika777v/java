package com.example.onboarding.security;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import com.example.onboarding.entity.Role;
import com.example.onboarding.entity.User;
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
 * Startup recovery with APP_ADMIN_PASSWORD_RESET=true, for when the only administrator forgot the password.
 * The initializer is created by hand with the settings of each case.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
class AdminPasswordRecoveryTest {

    private static final String EMAIL = "admin@example.com";
    private static final String FORGOTTEN = "forgotten-password";
    private static final String FROM_ENV = "password-from-env";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;

    @BeforeEach
    void setUpAdministrator() {
        admin = new User();
        admin.setName("管理者");
        admin.setEmail(EMAIL);
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode(FORGOTTEN));
        admin = userRepository.save(admin);
    }

    private void start(String email, String password, boolean reset) {
        new AdminAccountInitializer(userRepository, passwordEncoder, email, password, "管理者", reset).run(null);
    }

    private User reload(User user) {
        return userRepository.findById(user.getId()).orElseThrow();
    }

    @Test
    void resetSetsPasswordBackAndRequiresChange() {
        start(EMAIL, FROM_ENV, true);

        assertThat(passwordEncoder.matches(FROM_ENV, reload(admin).getPasswordHash())).isTrue();
        assertThat(reload(admin).isPasswordChangeRequired()).isTrue();
    }

    @Test
    void withoutResetExistingAdministratorIsLeftAlone() {
        start(EMAIL, FROM_ENV, false);

        assertThat(passwordEncoder.matches(FORGOTTEN, reload(admin).getPasswordHash())).isTrue();
        assertThat(reload(admin).isPasswordChangeRequired()).isFalse();
    }

    @Test
    void newEmployeeIsNeverReset() {
        User suzuki = userRepository.findById(3L).orElseThrow();

        start("suzuki@example.com", FROM_ENV, true);

        assertThat(reload(suzuki).getPasswordHash()).isNull();
        assertThat(reload(suzuki).isAdmin()).isFalse();
    }

    @Test
    void emptyPasswordResetsNothing() {
        start(EMAIL, "", true);

        assertThat(passwordEncoder.matches(FORGOTTEN, reload(admin).getPasswordHash())).isTrue();
    }

    @Test
    void firstAdministratorIsCreatedWithoutChangeRequired() {
        userRepository.delete(admin);

        start("new-admin@example.com", FROM_ENV, false);

        User created = userRepository.findByEmailIgnoreCase("new-admin@example.com").orElseThrow();
        assertThat(created.isAdmin()).isTrue();
        assertThat(created.isPasswordChangeRequired()).isFalse();
    }
}
