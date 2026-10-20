package com.example.onboarding.feature;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Answers whether a feature is on. The settings are read once on startup
 * (change the environment variable and restart to switch). Also used by templates
 * as ${@features.on('ticket-system')}.
 */
@Component("features")
public class FeatureFlags {

    private final Map<Feature, Boolean> settings = new EnumMap<>(Feature.class);

    public FeatureFlags(Environment environment) {
        for (Feature feature : Feature.values()) {
            settings.put(feature, environment.getProperty(
                    feature.propertyName(), Boolean.class, feature.isEnabledByDefault()));
        }
    }

    /** True when the feature and all its parents are on. */
    public boolean isEnabled(Feature feature) {
        return settings.get(feature) && (feature.getParent() == null || isEnabled(feature.getParent()));
    }

    /** For templates: on('ticket-system'). */
    public boolean on(String key) {
        return isEnabled(Feature.fromKey(key));
    }
}
