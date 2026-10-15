package com.example.onboarding.controller;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import com.example.onboarding.entity.Task;
import com.example.onboarding.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests for the screens (/, /tasks, /tasks/new, /tasks/{id}/edit, ...).
 * Each test runs in a transaction that is rolled back, so tests do not affect each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
class TaskPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    // --- list ---

    @Test
    void rootRedirectsToTaskList() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"));
    }

    @Test
    void listShowsSampleTasks() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks"))
                .andExpect(model().attribute("tasks", hasSize(5)))
                .andExpect(content().string(containsString("PCのセットアップ")))
                .andExpect(content().string(containsString("新規登録")));
    }

    // --- create ---

    @Test
    void newFormIsShown() throws Exception {
        mockMvc.perform(get("/tasks/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("task-form"))
                .andExpect(content().string(containsString("タスク登録")));
    }

    @Test
    void createSavesTaskAndRedirectsWithMessage() throws Exception {
        mockMvc.perform(post("/tasks")
                        .param("title", "  入社書類の提出  ")
                        .param("description", "人事に提出する")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"))
                .andExpect(flash().attribute("message", "タスクを登録しました。"));

        Task created = findByTitle("入社書類の提出");
        assertThat(created.getDescription()).isEqualTo("人事に提出する");
        assertThat(created.getActive()).isTrue();
    }

    @Test
    void createShowsErrorWhenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/tasks")
                        .param("title", " ")
                        .param("description", "no title"))
                .andExpect(status().isOk())
                .andExpect(view().name("task-form"))
                .andExpect(model().attributeHasFieldErrors("taskForm", "title"))
                .andExpect(content().string(containsString("タイトルを入力してください")));

        assertThat(taskRepository.count()).isEqualTo(5);
    }

    @Test
    void createShowsErrorWhenTitleIsTooLong() throws Exception {
        mockMvc.perform(post("/tasks")
                        .param("title", "あ".repeat(201)))
                .andExpect(status().isOk())
                .andExpect(view().name("task-form"))
                .andExpect(content().string(containsString("タイトルは200文字以内で入力してください")));

        assertThat(taskRepository.count()).isEqualTo(5);
    }

    @Test
    void createIgnoresIdSentFromForm() throws Exception {
        mockMvc.perform(post("/tasks")
                        .param("id", "1")
                        .param("title", "Should be a new task"))
                .andExpect(status().is3xxRedirection());

        assertThat(taskRepository.count()).isEqualTo(6);
        assertThat(taskRepository.findById(1L).orElseThrow().getTitle()).isEqualTo("PCのセットアップ");
    }

    // --- edit ---

    @Test
    void editFormShowsCurrentValues() throws Exception {
        mockMvc.perform(get("/tasks/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("task-form"))
                .andExpect(content().string(containsString("タスク編集")))
                .andExpect(content().string(containsString("value=\"PCのセットアップ\"")));
    }

    @Test
    void editFormReturns404WhenTaskDoesNotExist() throws Exception {
        mockMvc.perform(get("/tasks/9999/edit"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateChangesTaskAndRedirectsWithMessage() throws Exception {
        // An unchecked checkbox sends only the hidden "_active" field
        mockMvc.perform(post("/tasks/1")
                        .param("title", "PCのセットアップ(更新)")
                        .param("description", "")
                        .param("_active", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"))
                .andExpect(flash().attribute("message", "タスクを更新しました。"));

        Task task = taskRepository.findById(1L).orElseThrow();
        assertThat(task.getTitle()).isEqualTo("PCのセットアップ(更新)");
        assertThat(task.getDescription()).isNull();
        assertThat(task.getActive()).isFalse();
    }

    @Test
    void updateShowsErrorWhenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/tasks/1")
                        .param("title", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("task-form"))
                .andExpect(model().attribute("taskId", 1L))
                .andExpect(content().string(containsString("タイトルを入力してください")));

        assertThat(taskRepository.findById(1L).orElseThrow().getTitle()).isEqualTo("PCのセットアップ");
    }

    @Test
    void updateReturns404WhenTaskDoesNotExist() throws Exception {
        mockMvc.perform(post("/tasks/9999")
                        .param("title", "Does not matter"))
                .andExpect(status().isNotFound());
    }

    // --- delete ---

    @Test
    void deleteRemovesTaskAndRedirectsWithMessage() throws Exception {
        mockMvc.perform(post("/tasks/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"))
                .andExpect(flash().attribute("message", "タスクを削除しました。"));

        assertThat(taskRepository.existsById(1L)).isFalse();
    }

    @Test
    void deleteReturns404WhenTaskDoesNotExist() throws Exception {
        mockMvc.perform(post("/tasks/9999/delete"))
                .andExpect(status().isNotFound());
    }

    private Task findByTitle(String title) {
        return taskRepository.findAll().stream()
                .filter(task -> title.equals(task.getTitle()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Task not found: " + title));
    }
}
