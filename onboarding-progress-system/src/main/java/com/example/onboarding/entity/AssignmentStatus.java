package com.example.onboarding.entity;

/**
 * Progress of an assigned task. Stored in the database as the constant name (e.g. "IN_PROGRESS").
 */
public enum AssignmentStatus {

    NOT_STARTED("未着手"),
    IN_PROGRESS("対応中"),
    DONE("完了");

    private final String label;

    AssignmentStatus(String label) {
        this.label = label;
    }

    /** Japanese name shown on screens. */
    public String getLabel() {
        return label;
    }
}
