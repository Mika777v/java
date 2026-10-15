package com.example.onboarding;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Fixes "now" to 2026-10-15 09:00 (Asia/Tokyo) so that overdue checks give the same result
 * whenever the tests run. With the sample data, 佐藤's due dates (2026-04-10) are overdue and
 * 鈴木's (2026-10-20 / 2026-10-31) are not.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfiguration {

    public static final Instant NOW = Instant.parse("2026-10-15T00:00:00Z");

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW, ZoneId.of("Asia/Tokyo"));
    }
}
