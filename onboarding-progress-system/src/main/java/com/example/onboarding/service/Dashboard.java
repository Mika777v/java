package com.example.onboarding.service;

import com.example.onboarding.entity.TaskAssignment;
import com.example.onboarding.entity.User;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything shown on the dashboard (top page).
 *
 * @param userCount     number of users
 * @param overall       progress of all assignments together (total / done / overdue)
 * @param notStarted    number of assignments that are 未着手
 * @param inProgress    number of assignments that are 対応中
 * @param followUps     users with overdue tasks, most overdue first
 * @param upcoming      unfinished assignments due between today and upcomingUntil
 * @param today         "today" in the app time zone
 * @param upcomingUntil last day of the upcoming period
 */
public record Dashboard(
        long userCount,
        UserProgress overall,
        long notStarted,
        long inProgress,
        List<FollowUp> followUps,
        List<TaskAssignment> upcoming,
        LocalDate today,
        LocalDate upcomingUntil) {

    /** A user who needs attention, with their progress. */
    public record FollowUp(User user, UserProgress progress) {
    }
}
