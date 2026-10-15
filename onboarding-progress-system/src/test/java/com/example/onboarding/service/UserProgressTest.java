package com.example.onboarding.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProgressTest {

    @Test
    void percentIsRoundedDown() {
        assertThat(new UserProgress(3, 1).percent()).isEqualTo(33);
        assertThat(new UserProgress(4, 2).percent()).isEqualTo(50);
        assertThat(new UserProgress(4, 4).percent()).isEqualTo(100);
    }

    @Test
    void percentIsZeroWhenNothingIsAssigned() {
        assertThat(UserProgress.NONE.percent()).isZero();
    }
}
