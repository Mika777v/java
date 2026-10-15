package com.example.onboarding.web;

import com.example.onboarding.entity.TaskAssignment;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Helpers for templates, used as ${@dates.format(...)}.
 */
@Component("dates")
public class DateTimeDisplay {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Clock clock;

    public DateTimeDisplay(Clock clock) {
        this.clock = clock;
    }

    /** Formats a stored time in the app time zone, or returns "" when it is null. */
    public String format(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(instant.atZone(clock.getZone()));
    }

    public boolean isOverdue(TaskAssignment assignment) {
        return assignment.isOverdue(LocalDate.now(clock));
    }
}
