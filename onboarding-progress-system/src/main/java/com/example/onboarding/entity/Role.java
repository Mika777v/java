package com.example.onboarding.entity;

/**
 * What a user may do after logging in.
 */
public enum Role {

    /** Manages tasks, users and assignments; sees the dashboard. */
    ADMIN("管理者"),
    /** New employee: sees and updates only their own tasks on "マイページ". */
    MEMBER("新入社員");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
