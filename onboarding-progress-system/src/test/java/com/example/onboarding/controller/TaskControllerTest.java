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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the REST API (/api/tasks).
 * Each test runs in a transaction that is rolled back, so tests do not affect each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void listReturnsSampleTasks() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].title", hasItem("PCのセットアップ")));
    }

    @Test
    void getReturnsOneTask() throws Exception {
        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("PCのセットアップ"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getReturns404WhenTaskDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/tasks/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createSavesTask() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Set up laptop", "description": "Install tools", "active": true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title").value("Set up laptop"))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        assertThat(taskRepository.count()).isEqualTo(6);
    }

    @Test
    void createReturns400WhenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "", "description": "no title"}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(taskRepository.count()).isEqualTo(5);
    }

    @Test
    void createTreatsNullActiveAsActive() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Null active", "active": null}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createIgnoresIdAndTimestampsInBody() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": 1, "title": "Should be new", "createdAt": "2000-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(1)))
                .andExpect(jsonPath("$.createdAt").value(not(startsWith("2000"))));

        assertThat(taskRepository.count()).isEqualTo(6);
        assertThat(taskRepository.findById(1L).orElseThrow().getTitle()).isEqualTo("PCのセットアップ");
    }

    @Test
    void createReturns400WhenTitleIsTooLong() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + "a".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest());

        assertThat(taskRepository.count()).isEqualTo(5);
    }

    @Test
    void updateReturns400WhenTitleIsTooLong() throws Exception {
        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + "a".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateChangesTask() throws Exception {
        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Updated title", "description": "Updated description", "active": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.active").value(false));

        Task task = taskRepository.findById(1L).orElseThrow();
        assertThat(task.getTitle()).isEqualTo("Updated title");
        assertThat(task.getDescription()).isEqualTo("Updated description");
        assertThat(task.getActive()).isFalse();
    }

    @Test
    void updateReturns404WhenTaskDoesNotExist() throws Exception {
        mockMvc.perform(put("/api/tasks/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Updated title"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesTask() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());

        assertThat(taskRepository.existsById(1L)).isFalse();
    }

    @Test
    void deleteReturns404WhenTaskDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/tasks/9999"))
                .andExpect(status().isNotFound());
    }
}
