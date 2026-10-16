package com.example.onboarding.controller;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import com.example.onboarding.entity.AssignmentStatus;
import com.example.onboarding.entity.TaskAssignment;
import com.example.onboarding.repository.TaskAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static com.example.onboarding.WebTestSupport.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests for the user detail screen: assigning tasks and changing their progress.
 * Sample data: 鈴木 (user 3) has tasks 1 (対応中) and 2 (未着手); tasks 3, 4 and 6 are not assigned to him.
 * Task 5 is inactive. Task 6 is the sample task with a link (V9), not assigned to anyone.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
@WithMockUser(roles = "ADMIN")
class UserAssignmentPageTest {

    private static final long SUZUKI = 3L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskAssignmentRepository assignmentRepository;

    private List<TaskAssignment> assignmentsOf(long userId) {
        return assignmentRepository.findByUserIdOrderByTaskIdAsc(userId);
    }

    // --- detail ---

    @Test
    void detailShowsAssignmentsAndAssignableTasks() throws Exception {
        mockMvc.perform(get("/users/3"))
                .andExpect(status().isOk())
                .andExpect(view().name("user-detail"))
                .andExpect(model().attribute("assignments", hasSize(2)))
                .andExpect(model().attribute("assignableTasks", hasSize(3)))
                .andExpect(content().string(containsString("鈴木 一郎 さんの進捗")))
                .andExpect(content().string(containsString("0 / 2 完了(0%)")))
                .andExpect(content().string(containsString("有効なタスクをすべて割り当てる(3件)")));
    }

    @Test
    void detailReturns404WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/users/9999"))
                .andExpect(status().isNotFound());
    }

    // --- assign ---

    @Test
    void assignAddsTaskWithDueDate() throws Exception {
        mockMvc.perform(post("/users/3/assignments")
                        .param("taskId", "3")
                        .param("dueDate", "2026-11-15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/3"))
                .andExpect(flash().attribute("message", "「就業規則の確認」を割り当てました。"));

        TaskAssignment added = assignmentsOf(SUZUKI).get(2);
        assertThat(added.getTask().getId()).isEqualTo(3L);
        assertThat(added.getStatus()).isEqualTo(AssignmentStatus.NOT_STARTED);
        assertThat(added.getDueDate()).isEqualTo(LocalDate.of(2026, 11, 15));
    }

    @Test
    void assignShowsErrorWhenNoTaskIsSelected() throws Exception {
        mockMvc.perform(post("/users/3/assignments").param("taskId", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "割り当てるタスクを選択してください。"));

        assertThat(assignmentsOf(SUZUKI)).hasSize(2);
    }

    @Test
    void assignShowsErrorWhenAlreadyAssigned() throws Exception {
        mockMvc.perform(post("/users/3/assignments").param("taskId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "このタスクは既に割り当てられています。"));

        assertThat(assignmentsOf(SUZUKI)).hasSize(2);
    }

    @Test
    void assignShowsErrorForInactiveTask() throws Exception {
        mockMvc.perform(post("/users/3/assignments").param("taskId", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "無効(Active: No)のタスクは割り当てられません。"));

        assertThat(assignmentsOf(SUZUKI)).hasSize(2);
    }

    @Test
    void assignAllAddsEveryActiveTaskNotYetAssigned() throws Exception {
        mockMvc.perform(post("/users/3/assignments/all").param("dueDate", "2026-11-30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "3件のタスクを割り当てました。"));

        List<TaskAssignment> assignments = assignmentsOf(SUZUKI);
        assertThat(assignments).extracting(a -> a.getTask().getId()).containsExactly(1L, 2L, 3L, 4L, 6L);
        assertThat(assignments.get(4).getDueDate()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void assignAllShowsErrorWhenNothingIsLeft() throws Exception {
        // 山田 (user 1) has every active task except the sample task 6; after assigning it, nothing is left
        mockMvc.perform(post("/users/1/assignments").param("taskId", "6"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/users/1/assignments/all"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "割り当てられる有効なタスクはありません。"));
    }

    // --- progress ---

    @Test
    void changingStatusToDoneRecordsCompletionTime() throws Exception {
        TaskAssignment assignment = assignmentsOf(SUZUKI).get(0);

        mockMvc.perform(post("/users/3/assignments/{aid}/status", assignment.getId())
                        .param("status", "DONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/3"))
                .andExpect(flash().attribute("message", "「PCのセットアップ」を「完了」にしました。"));

        TaskAssignment updated = assignmentRepository.findById(assignment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AssignmentStatus.DONE);
        assertThat(updated.getCompletedAt()).isNotNull();

        mockMvc.perform(get("/users/3"))
                .andExpect(content().string(containsString("1 / 2 完了(50%)")));
    }

    @Test
    void changingStatusBackFromDoneClearsCompletionTime() throws Exception {
        // 佐藤 (user 2): task 1 is done
        TaskAssignment assignment = assignmentsOf(2L).get(0);
        assertThat(assignment.getCompletedAt()).isNotNull();

        mockMvc.perform(post("/users/2/assignments/{aid}/status", assignment.getId())
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().is3xxRedirection());

        TaskAssignment updated = assignmentRepository.findById(assignment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AssignmentStatus.IN_PROGRESS);
        assertThat(updated.getCompletedAt()).isNull();
    }

    @Test
    void changingStatusOfAnotherUsersAssignmentReturns404() throws Exception {
        TaskAssignment yamadasAssignment = assignmentsOf(1L).get(0);

        mockMvc.perform(post("/users/3/assignments/{aid}/status", yamadasAssignment.getId())
                        .param("status", "NOT_STARTED"))
                .andExpect(status().isNotFound());

        assertThat(assignmentRepository.findById(yamadasAssignment.getId()).orElseThrow().getStatus())
                .isEqualTo(AssignmentStatus.DONE);
    }

    @Test
    void changingStatusToUnknownValueReturns400() throws Exception {
        TaskAssignment assignment = assignmentsOf(SUZUKI).get(0);

        mockMvc.perform(post("/users/3/assignments/{aid}/status", assignment.getId())
                        .param("status", "FINISHED"))
                .andExpect(status().isBadRequest());
    }

    // --- unassign ---

    @Test
    void unassignRemovesAssignment() throws Exception {
        TaskAssignment assignment = assignmentsOf(SUZUKI).get(1);

        mockMvc.perform(post("/users/3/assignments/{aid}/delete", assignment.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "「社内アカウントの登録」の割り当てを解除しました。"));

        assertThat(assignmentsOf(SUZUKI)).extracting(a -> a.getTask().getId()).containsExactly(1L);
    }

    @Test
    void unassignReturns404ForAnotherUsersAssignment() throws Exception {
        TaskAssignment yamadasAssignment = assignmentsOf(1L).get(0);

        mockMvc.perform(post("/users/3/assignments/{aid}/delete", yamadasAssignment.getId()))
                .andExpect(status().isNotFound());

        assertThat(assignmentRepository.existsById(yamadasAssignment.getId())).isTrue();
    }

    // --- deleting a task ---

    @Test
    void deletingTaskAlsoRemovesItsAssignments() throws Exception {
        mockMvc.perform(post("/tasks/1/delete"))
                .andExpect(status().is3xxRedirection());

        assertThat(assignmentsOf(1L)).extracting(a -> a.getTask().getId()).containsExactly(2L, 3L, 4L);
        assertThat(assignmentsOf(SUZUKI)).extracting(a -> a.getTask().getId()).containsExactly(2L);
    }
}
