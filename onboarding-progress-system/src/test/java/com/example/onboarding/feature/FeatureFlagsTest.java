package com.example.onboarding.feature;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeatureFlagsTest {

    @Test
    void newFeaturesAreOffAndOnboardingIsOnByDefault() {
        FeatureFlags features = new FeatureFlags(new MockEnvironment());

        for (Feature feature : Feature.values()) {
            assertThat(features.isEnabled(feature))
                    .as(feature.getKey())
                    .isEqualTo(feature == Feature.ONBOARDING_LEGACY);
        }
    }

    @Test
    void featureCanBeSwitchedOnWithItsSetting() {
        FeatureFlags features = new FeatureFlags(new MockEnvironment()
                .withProperty("features.ticket-system.enabled", "true")
                .withProperty("features.onboarding-legacy.enabled", "false"));

        assertThat(features.isEnabled(Feature.TICKET_SYSTEM)).isTrue();
        assertThat(features.isEnabled(Feature.ONBOARDING_LEGACY)).isFalse();
    }

    @Test
    void childStaysOffWhileParentIsOff() {
        FeatureFlags features = new FeatureFlags(new MockEnvironment()
                .withProperty("features.kanban-board.enabled", "true"));

        assertThat(features.isEnabled(Feature.KANBAN_BOARD)).isFalse();
    }

    @Test
    void childIsOnWhenItAndItsParentAreOn() {
        FeatureFlags features = new FeatureFlags(new MockEnvironment()
                .withProperty("features.ticket-system.enabled", "true")
                .withProperty("features.kanban-board.enabled", "true"));

        assertThat(features.isEnabled(Feature.KANBAN_BOARD)).isTrue();
        assertThat(features.isEnabled(Feature.COMMENTS)).isFalse();
    }

    @Test
    void templatesAskByKey() {
        FeatureFlags features = new FeatureFlags(new MockEnvironment()
                .withProperty("features.ticket-system.enabled", "true"));

        assertThat(features.on("ticket-system")).isTrue();
        assertThat(features.on("comments")).isFalse();
    }

    @Test
    void unknownKeyFailsInsteadOfReadingAsOff() {
        FeatureFlags features = new FeatureFlags(new MockEnvironment());

        assertThatThrownBy(() -> features.on("ticket-sytem"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ticket-sytem");
    }

    @Test
    void invalidSettingStopsStartup() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("features.ticket-system.enabled", "maybe");

        assertThatThrownBy(() -> new FeatureFlags(environment)).isInstanceOf(RuntimeException.class);
    }
}
