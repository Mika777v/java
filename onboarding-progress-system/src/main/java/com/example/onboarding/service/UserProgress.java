package com.example.onboarding.service;

/**
 * How many of a user's assigned tasks are done, and how many are overdue.
 */
public record UserProgress(long total, long done, long overdue) {

    public static final UserProgress NONE = new UserProgress(0, 0, 0);

    /** Completion rate from 0 to 100 (0 when nothing is assigned). */
    public int percent() {
        return total == 0 ? 0 : (int) (done * 100 / total);
    }

    public boolean isUnassigned() {
        return total == 0;
    }

    public boolean isAllDone() {
        return total > 0 && done == total;
    }

    public boolean hasIncomplete() {
        return done < total;
    }

    public boolean hasOverdue() {
        return overdue > 0;
    }
}
