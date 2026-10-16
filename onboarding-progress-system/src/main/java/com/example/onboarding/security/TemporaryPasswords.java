package com.example.onboarding.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Random temporary passwords, e.g. "k7Hw3pQx9mTa", for resetting someone's password.
 * Letters and digits that are easy to confuse (0/O, 1/l/I) are left out, so it can be read out or typed.
 */
@Component
public class TemporaryPasswords {

    private static final String CHARACTERS = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder password = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            password.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return password.toString();
    }
}
