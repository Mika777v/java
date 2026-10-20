package com.example.onboarding.feature;

import java.util.Arrays;

/**
 * Feature toggles for the move to ticket management (docs/FEATURE_TOGGLE_STRATEGY.md).
 * Each one is switched with features.&lt;key&gt;.enabled (environment variable FEATURE_&lt;KEY&gt;).
 * A feature with a parent counts as off while its parent is off.
 */
public enum Feature {
    TICKET_SYSTEM("ticket-system", null, false),
    PROJECT_MANAGEMENT("project-management", TICKET_SYSTEM, false),
    COMMENTS("comments", TICKET_SYSTEM, false),
    NOTIFICATIONS("notifications", TICKET_SYSTEM, false),
    EXTENDED_ROLES("extended-roles", TICKET_SYSTEM, false),
    TICKET_DASHBOARD("ticket-dashboard", TICKET_SYSTEM, false),
    KANBAN_BOARD("kanban-board", TICKET_SYSTEM, false),
    // The current onboarding screens: on until they are retired in Phase10
    ONBOARDING_LEGACY("onboarding-legacy", null, true);

    private final String key;
    private final Feature parent;
    private final boolean enabledByDefault;

    Feature(String key, Feature parent, boolean enabledByDefault) {
        this.key = key;
        this.parent = parent;
        this.enabledByDefault = enabledByDefault;
    }

    public String getKey() {
        return key;
    }

    public Feature getParent() {
        return parent;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    /** Name of the setting, e.g. features.ticket-system.enabled. */
    public String propertyName() {
        return "features." + key + ".enabled";
    }

    /** Finds a feature by its key; an unknown key is a typo, so it fails instead of reading as off. */
    public static Feature fromKey(String key) {
        return Arrays.stream(values())
                .filter(feature -> feature.key.equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown feature: " + key));
    }
}
