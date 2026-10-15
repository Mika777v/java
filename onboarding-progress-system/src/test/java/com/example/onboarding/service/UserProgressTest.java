package com.example.onboarding.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProgressTest {

    @Test
    void percentIsRoundedDown() {
        assertThat(new UserProgress(3, 1, 0).percent()).isEqualTo(33);
        assertThat(new UserProgress(4, 2, 0).percent()).isEqualTo(50);
        assertThat(new UserProgress(4, 4, 0).percent()).isEqualTo(100);
    }

    @Test
    void percentIsZeroWhenNothingIsAssigned() {
        assertThat(UserProgress.NONE.percent()).isZero();
    }

    @Test
    void conditions() {
        UserProgress halfDone = new UserProgress(4, 2, 1);
        assertThat(halfDone.hasIncomplete()).isTrue();
        assertThat(halfDone.hasOverdue()).isTrue();
        assertThat(halfDone.isAllDone()).isFalse();
        assertThat(halfDone.isUnassigned()).isFalse();

        UserProgress allDone = new UserProgress(4, 4, 0);
        assertThat(allDone.hasIncomplete()).isFalse();
        assertThat(allDone.isAllDone()).isTrue();

        assertThat(UserProgress.NONE.isUnassigned()).isTrue();
        assertThat(UserProgress.NONE.isAllDone()).isFalse();
        assertThat(UserProgress.NONE.hasIncomplete()).isFalse();
    }

    @Test
    void filterMatchesProgress() {
        UserProgress halfDone = new UserProgress(4, 2, 1);
        UserProgress allDone = new UserProgress(4, 4, 0);

        assertThat(ProgressFilter.INCOMPLETE.matches(halfDone)).isTrue();
        assertThat(ProgressFilter.INCOMPLETE.matches(allDone)).isFalse();
        assertThat(ProgressFilter.OVERDUE.matches(halfDone)).isTrue();
        assertThat(ProgressFilter.OVERDUE.matches(allDone)).isFalse();
        assertThat(ProgressFilter.OVERDUE.matches(new UserProgress(4, 2, 0))).isFalse();
        assertThat(ProgressFilter.COMPLETED.matches(allDone)).isTrue();
        assertThat(ProgressFilter.COMPLETED.matches(UserProgress.NONE)).isFalse();
        assertThat(ProgressFilter.UNASSIGNED.matches(UserProgress.NONE)).isTrue();
        assertThat(ProgressFilter.UNASSIGNED.matches(halfDone)).isFalse();
    }
}
