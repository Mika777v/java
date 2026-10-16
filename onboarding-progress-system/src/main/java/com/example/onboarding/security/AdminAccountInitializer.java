package com.example.onboarding.security;

import com.example.onboarding.entity.Role;
import com.example.onboarding.entity.User;
import com.example.onboarding.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator on startup, so that someone can log in.
 * The password comes from the environment (APP_ADMIN_PASSWORD) and is never written in the code.
 * Nothing happens once an administrator exists.
 *
 * <p>Recovery when the only administrator forgot the password: start once with
 * APP_ADMIN_PASSWORD_RESET=true. The password of the administrator APP_ADMIN_EMAIL is set back to
 * APP_ADMIN_PASSWORD and must be changed after logging in. Remove the setting afterwards,
 * otherwise the password is set back on every start.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    // Same rule as the password forms (BCrypt uses only the first 72 bytes)
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;
    private final boolean resetPassword;

    public AdminAccountInitializer(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${app.admin.email:admin@example.com}") String email,
                                   @Value("${app.admin.password:}") String password,
                                   @Value("${app.admin.name:管理者}") String name,
                                   @Value("${app.admin.password-reset:false}") boolean resetPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email.strip();
        this.password = password;
        this.name = name;
        this.resetPassword = resetPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (resetPassword) {
            resetAdministratorPassword();
        }
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (password == null || password.isBlank()) {
            log.warn("No administrator exists. Set APP_ADMIN_PASSWORD (and optionally APP_ADMIN_EMAIL) "
                    + "to create one on startup.");
            return;
        }
        if (!hasValidLength(password)) {
            log.error("No administrator was created: APP_ADMIN_PASSWORD must be {}-{} characters.",
                    MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH);
            return;
        }

        User admin = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            return user;
        });
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(admin);
        log.info("Created administrator {}", email);
    }

    private void resetAdministratorPassword() {
        if (password == null || password.isBlank()) {
            log.warn("APP_ADMIN_PASSWORD_RESET is set, but APP_ADMIN_PASSWORD is empty: nothing was reset.");
            return;
        }
        if (!hasValidLength(password)) {
            log.error("APP_ADMIN_PASSWORD_RESET is set, but APP_ADMIN_PASSWORD must be {}-{} characters: "
                    + "nothing was reset.", MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH);
            return;
        }
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(user -> {
            if (!user.isAdmin()) {
                log.warn("APP_ADMIN_PASSWORD_RESET is set, but {} is not an administrator: nothing was reset.", email);
                return;
            }
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setPasswordChangeRequired(true);
            userRepository.save(user);
            log.warn("The password of administrator {} was reset to APP_ADMIN_PASSWORD. Log in, change it, "
                    + "and then remove APP_ADMIN_PASSWORD_RESET (otherwise it is reset on every start).", email);
        }, () -> log.warn("APP_ADMIN_PASSWORD_RESET is set, but no user {} exists: nothing was reset.", email));
    }

    private static boolean hasValidLength(String password) {
        return password.length() >= MIN_PASSWORD_LENGTH && password.length() <= MAX_PASSWORD_LENGTH;
    }
}
