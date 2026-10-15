package com.example.onboarding.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * The clock used for "now" and "today" (completion time, overdue check, display).
 * Times are stored in UTC; app.time-zone decides how they are shown and what "today" is.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(@Value("${app.time-zone:Asia/Tokyo}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
