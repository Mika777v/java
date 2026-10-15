package com.example.onboarding.controller;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import com.example.onboarding.entity.AssignmentStatus;
import com.example.onboarding.entity.TaskAssignment;
import com.example.onboarding.repository.TaskAssignmentRepository;
import com.example.onboarding.service.Dashboard;
import com.example.onboarding.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests for the dashboard (top page). "Today" is fixed to 2026-10-15.
 * Sample data: 10 assignments — 6 done, 2 in progress, 2 not started.
 * 佐藤 has 2 overdue tasks (due 2026-04-10); 鈴木 has tasks due 2026-10-20 and 2026-10-31.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
class DashboardTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private TaskAssignmentRepository assignmentRepository;

    @Test
    void numbersAreCalculatedFromAssignments() {
        Dashboard dashboard = dashboardService.build();

        assertThat(dashboard.userCount()).isEqualTo(3);
        assertThat(dashboard.overall().total()).isEqualTo(10);
        assertThat(dashboard.overall().done()).isEqualTo(6);
        assertThat(dashboard.overall().percent()).isEqualTo(60);
        assertThat(dashboard.overall().overdue()).isEqualTo(2);
        assertThat(dashboard.inProgress()).isEqualTo(2);
        assertThat(dashboard.notStarted()).isEqualTo(2);
    }

    @Test
    void followUpsListOnlyUsersWithOverdueTasks() {
        Dashboard dashboard = dashboardService.build();

        assertThat(dashboard.followUps())
                .extracting(f -> f.user().getName())
                .containsExactly("佐藤 花子");
        assertThat(dashboard.followUps().get(0).progress().overdue()).isEqualTo(2);
    }

    @Test
    void upcomingListsUnfinishedTasksDueWithinSevenDays() {
        Dashboard dashboard = dashboardService.build();

        assertThat(dashboard.today()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(dashboard.upcomingUntil()).isEqualTo(LocalDate.of(2026, 10, 22));
        // 鈴木's task due 2026-10-20 is listed; the one due 2026-10-31 is too far away
        assertThat(dashboard.upcoming())
                .extracting(a -> a.getUser().getName() + " / " + a.getTask().getTitle() + " / " + a.getDueDate())
                .containsExactly("鈴木 一郎 / PCのセットアップ / 2026-10-20");
    }

    @Test
    void completedTaskDisappearsFromUpcoming() throws Exception {
        TaskAssignment dueSoon = assignmentRepository.findByUserIdOrderByTaskIdAsc(3L).get(0);

        mockMvc.perform(post("/users/3/assignments/{aid}/status", dueSoon.getId())
                .param("status", AssignmentStatus.DONE.name()));

        assertThat(dashboardService.build().upcoming()).isEmpty();
    }

    @Test
    void rootShowsDashboard() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(containsString("ダッシュボード")))
                .andExpect(content().string(containsString("60%")))
                .andExpect(content().string(containsString("6 / 10 件完了")))
                .andExpect(content().string(containsString("佐藤 花子")))
                .andExpect(content().string(containsString("期限が近いタスク(10/22 まで)")))
                .andExpect(content().string(containsString("2026-10-20")));
    }
}
