package com.example.onboarding.service;

import com.example.onboarding.entity.User;
import com.example.onboarding.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Personal links for new employees (/join/{token}).
 * The token is 32 random bytes (43 characters in the URL), so it cannot be guessed.
 * Only its SHA-256 hash is saved: the link is shown once when it is issued and cannot be read back later.
 */
@Service
@Transactional
public class AccessLinkService {

    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final Clock clock;
    private final Duration validity;
    private final SecureRandom random = new SecureRandom();

    public AccessLinkService(UserRepository userRepository,
                             Clock clock,
                             @Value("${app.access-link.valid-days:60}") int validDays) {
        this.userRepository = userRepository;
        this.clock = clock;
        this.validity = Duration.ofDays(validDays);
    }

    /**
     * Creates a new link for the user and returns its token. An older link stops working.
     * Only new employees get links; administrators log in with their password.
     */
    public String issue(User user) {
        if (user.isAdmin()) {
            throw new IllegalArgumentException("Administrators cannot have a personal link");
        }
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        user.setAccessLink(hash(token), Instant.now(clock).plus(validity));
        userRepository.save(user);
        return token;
    }

    public void revoke(User user) {
        user.clearAccessLink();
        userRepository.save(user);
    }

    /** The new employee the link belongs to, if the link exists and has not expired. */
    @Transactional(readOnly = true)
    public Optional<User> findUser(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        return userRepository.findByAccessTokenHash(hash(token))
                .filter(user -> !user.isAdmin())
                .filter(user -> user.hasValidAccessLink(now));
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
