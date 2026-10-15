package com.example.onboarding.service;

/**
 * Progress conditions for narrowing down the user list (/users?status=...).
 */
public enum ProgressFilter {

    INCOMPLETE("未完了あり"),
    OVERDUE("期限切れあり"),
    COMPLETED("すべて完了"),
    UNASSIGNED("未割り当て");

    private final String label;

    ProgressFilter(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean matches(UserProgress progress) {
        return switch (this) {
            case INCOMPLETE -> progress.hasIncomplete();
            case OVERDUE -> progress.hasOverdue();
            case COMPLETED -> progress.isAllDone();
            case UNASSIGNED -> progress.isUnassigned();
        };
    }
}
