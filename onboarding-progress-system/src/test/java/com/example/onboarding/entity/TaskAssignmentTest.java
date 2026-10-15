package com.example.onboarding.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the progress rules of TaskAssignment (no database or Spring needed).
 */
class TaskAssignmentTest {

    private static final Instant MONDAY = Instant.parse("2026-10-12T01:00:00Z");
    private static final Instant TUESDAY = Instant.parse("2026-10-13T01:00:00Z");

    private TaskAssignment newAssignment(LocalDate dueDate) {
        return new TaskAssignment(new User(), new Task(), dueDate);
    }

    @Test
    void newAssignmentIsNotStarted() {
        TaskAssignment assignment = newAssignment(null);

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.NOT_STARTED);
        assertThat(assignment.getCompletedAt()).isNull();
    }

    @Test
    void completingRecordsCompletionTime() {
        TaskAssignment assignment = newAssignment(null);

        assignment.changeStatus(AssignmentStatus.DONE, MONDAY);

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.DONE);
        assertThat(assignment.getCompletedAt()).isEqualTo(MONDAY);
    }

    @Test
    void completingAgainKeepsFirstCompletionTime() {
        TaskAssignment assignment = newAssignment(null);
        assignment.changeStatus(AssignmentStatus.DONE, MONDAY);

        assignment.changeStatus(AssignmentStatus.DONE, TUESDAY);

        assertThat(assignment.getCompletedAt()).isEqualTo(MONDAY);
    }

    @Test
    void reopeningClearsCompletionTime() {
        TaskAssignment assignment = newAssignment(null);
        assignment.changeStatus(AssignmentStatus.DONE, MONDAY);

        assignment.changeStatus(AssignmentStatus.IN_PROGRESS, TUESDAY);

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.IN_PROGRESS);
        assertThat(assignment.getCompletedAt()).isNull();
    }

    @Test
    void overdueWhenDueDatePassedAndNotDone() {
        LocalDate today = LocalDate.of(2026, 10, 15);

        assertThat(newAssignment(LocalDate.of(2026, 10, 14)).isOverdue(today)).isTrue();
        assertThat(newAssignment(LocalDate.of(2026, 10, 15)).isOverdue(today)).isFalse();
        assertThat(newAssignment(null).isOverdue(today)).isFalse();
    }

    @Test
    void doneTaskIsNeverOverdue() {
        TaskAssignment assignment = newAssignment(LocalDate.of(2026, 10, 1));
        assignment.changeStatus(AssignmentStatus.DONE, MONDAY);

        assertThat(assignment.isOverdue(LocalDate.of(2026, 10, 15))).isFalse();
    }
}
