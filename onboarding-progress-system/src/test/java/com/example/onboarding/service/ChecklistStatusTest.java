package com.example.onboarding.service;

import org.junit.jupiter.api.Test;

import static com.example.onboarding.entity.AssignmentStatus.DONE;
import static com.example.onboarding.entity.AssignmentStatus.IN_PROGRESS;
import static com.example.onboarding.entity.AssignmentStatus.NOT_STARTED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * How checking sub-items moves an assignment: some checked -> 対応中, all checked -> 完了.
 */
class ChecklistStatusTest {

    @Test
    void firstCheckStartsTheTask() {
        assertThat(ChecklistService.statusAfterCheck(NOT_STARTED, 1, 3)).isEqualTo(IN_PROGRESS);
    }

    @Test
    void allChecksCompleteTheTask() {
        assertThat(ChecklistService.statusAfterCheck(IN_PROGRESS, 3, 3)).isEqualTo(DONE);
        assertThat(ChecklistService.statusAfterCheck(NOT_STARTED, 1, 1)).isEqualTo(DONE);
    }

    @Test
    void uncheckingACompletedTaskGoesBackToInProgress() {
        assertThat(ChecklistService.statusAfterCheck(DONE, 2, 3)).isEqualTo(IN_PROGRESS);
        assertThat(ChecklistService.statusAfterCheck(DONE, 0, 1)).isEqualTo(IN_PROGRESS);
    }

    @Test
    void noChecksKeepsTheStatusThatWasChosenByHand() {
        assertThat(ChecklistService.statusAfterCheck(NOT_STARTED, 0, 3)).isEqualTo(NOT_STARTED);
        assertThat(ChecklistService.statusAfterCheck(IN_PROGRESS, 0, 3)).isEqualTo(IN_PROGRESS);
    }
}
