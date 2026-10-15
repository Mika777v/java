package com.example.onboarding.service;

/**
 * An assignment request that cannot be carried out. The message is shown to the user as is.
 */
public class AssignmentException extends RuntimeException {

    public AssignmentException(String message) {
        super(message);
    }
}
