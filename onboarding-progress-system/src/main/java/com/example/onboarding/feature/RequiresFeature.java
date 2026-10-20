package com.example.onboarding.feature;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put on a controller class or method: while the feature is off, its pages answer 404
 * (as if they did not exist yet). Checked by {@link FeatureToggleInterceptor}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresFeature {

    Feature value();
}
