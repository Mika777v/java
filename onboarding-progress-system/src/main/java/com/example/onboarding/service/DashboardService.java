package com.example.onboarding.service;

import com.example.onboarding.entity.AssignmentStatus;
import com.example.onboarding.entity.User;
import com.example.onboarding.repository.TaskAssignmentRepository;
import com.example.onboarding.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the numbers and lists for the dashboard.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** Tasks due within this many days from today are listed as "期限が近いタスク". */
    static final int UPCOMING_DAYS = 7;

    private final UserRepository userRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final AssignmentService assignmentService;
    private final Clock clock;

    public DashboardService(UserRepository userRepository,
                            TaskAssignmentRepository assignmentRepository,
                            AssignmentService assignmentService,
                            Clock clock) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentService = assignmentService;
        this.clock = clock;
    }

    public Dashboard build() {
        LocalDate today = LocalDate.now(clock);
        LocalDate upcomingUntil = today.plusDays(UPCOMING_DAYS);

        Map<AssignmentStatus, Long> counts = new EnumMap<>(AssignmentStatus.class);
        for (AssignmentStatus status : AssignmentStatus.values()) {
            counts.put(status, 0L);
        }
        assignmentRepository.countByStatus().forEach(row -> counts.put(row.getStatus(), row.getCount()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        Map<Long, UserProgress> progressByUser = assignmentService.progressByUser();
        long overdue = progressByUser.values().stream().mapToLong(UserProgress::overdue).sum();

        List<User> users = userRepository.findAll().stream().filter(user -> !user.isAdmin()).toList();
        List<Dashboard.FollowUp> followUps = users.stream()
                .map(user -> new Dashboard.FollowUp(user, progressByUser.getOrDefault(user.getId(), UserProgress.NONE)))
                .filter(followUp -> followUp.progress().hasOverdue())
                .sorted(Comparator.comparingLong((Dashboard.FollowUp f) -> f.progress().overdue()).reversed()
                        .thenComparingInt(f -> f.progress().percent())
                        .thenComparing(f -> f.user().getId()))
                .toList();

        return new Dashboard(
                users.size(),
                new UserProgress(total, counts.get(AssignmentStatus.DONE), overdue),
                counts.get(AssignmentStatus.NOT_STARTED),
                counts.get(AssignmentStatus.IN_PROGRESS),
                followUps,
                assignmentRepository.findByStatusNotAndDueDateBetweenOrderByDueDateAscIdAsc(
                        AssignmentStatus.DONE, today, upcomingUntil),
                today,
                upcomingUntil);
    }
}
