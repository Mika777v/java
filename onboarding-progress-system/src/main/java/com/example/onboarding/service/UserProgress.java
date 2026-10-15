package com.example.onboarding.service;

/**
 * How many of a user's assigned tasks are done.
 */
public record UserProgress(long total, long done) {

    public static final UserProgress NONE = new UserProgress(0, 0);

    /** Completion rate from 0 to 100 (0 when nothing is assigned). */
    public int percent() {
        return total == 0 ? 0 : (int) (done * 100 / total);
    }
}
