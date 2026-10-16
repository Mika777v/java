package com.example.onboarding.controller;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import com.example.onboarding.entity.Task;
import com.example.onboarding.entity.TaskAssignment;
import com.example.onboarding.repository.TaskRepository;
import com.example.onboarding.service.AssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static com.example.onboarding.WebTestSupport.post;
import static com.example.onboarding.WebTestSupport.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the display order of tasks (↑ / ↓ buttons on the task list).
 * Sample data starts in the order 1, 2, 3, 4, 5.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
@WithMockUser(roles = "ADMIN")
class TaskOrderTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AssignmentService assignmentService;

    private List<Long> displayedTaskIds() {
        return taskRepository.findAllInDisplayOrder().stream().map(Task::getId).toList();
    }

    @Test
    void sampleTasksKeepTheirOriginalOrder() {
        assertThat(displayedTaskIds()).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
    }

    @Test
    void moveDownSwapsWithNextTask() throws Exception {
        mockMvc.perform(post("/tasks/1/move-down"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"))
                .andExpect(flash().attribute("message", "「PCのセットアップ」を下に移動しました。"));

        assertThat(displayedTaskIds()).containsExactly(2L, 1L, 3L, 4L, 5L, 6L);
    }

    @Test
    void moveUpSwapsWithPreviousTask() throws Exception {
        mockMvc.perform(post("/tasks/4/move-up"))
                .andExpect(flash().attribute("message", "「チームメンバーとの顔合わせ」を上に移動しました。"));

        assertThat(displayedTaskIds()).containsExactly(1L, 2L, 4L, 3L, 5L, 6L);
    }

    @Test
    void firstTaskCannotMoveUpAndLastTaskCannotMoveDown() throws Exception {
        mockMvc.perform(post("/tasks/1/move-up"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeCount(0));
        mockMvc.perform(post("/tasks/6/move-down"))
                .andExpect(flash().attributeCount(0));

        assertThat(displayedTaskIds()).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
    }

    @Test
    void movingUnknownTaskReturns404() throws Exception {
        mockMvc.perform(post("/tasks/9999/move-up"))
                .andExpect(status().isNotFound());
    }

    @Test
    void taskCreatedOnScreenIsAddedAtTheEnd() throws Exception {
        mockMvc.perform(post("/tasks").param("title", "最後に追加").param("_active", "on"))
                .andExpect(status().is3xxRedirection());

        List<Task> tasks = taskRepository.findAllInDisplayOrder();
        assertThat(tasks.get(tasks.size() - 1).getTitle()).isEqualTo("最後に追加");
    }

    @Test
    void taskCreatedByApiWithoutOrderIsAddedAtTheEnd() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"API task\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sortOrder").value(70));
    }

    @Test
    void apiUpdateWithoutOrderKeepsTheOrder() throws Exception {
        mockMvc.perform(put("/api/tasks/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortOrder").value(20));
    }

    @Test
    void apiListUsesDisplayOrder() throws Exception {
        mockMvc.perform(post("/tasks/2/move-up"));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[1].id").value(1));
    }

    @Test
    void userAssignmentsAndAssignableTasksFollowDisplayOrder() throws Exception {
        // 鈴木 (user 3) has tasks 1 and 2; tasks 3, 4 and 6 (the sample link task) can still be assigned
        mockMvc.perform(post("/tasks/2/move-up"));
        mockMvc.perform(post("/tasks/4/move-up"));

        List<TaskAssignment> assignments = assignmentService.findAssignments(3L);
        assertThat(assignments).extracting(a -> a.getTask().getId()).containsExactly(2L, 1L);
        assertThat(assignmentService.findAssignableTasks(3L)).extracting(Task::getId).containsExactly(4L, 3L, 6L);
    }
}
