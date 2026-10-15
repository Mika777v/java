package com.example.onboarding.controller;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import com.example.onboarding.entity.User;
import com.example.onboarding.repository.TaskAssignmentRepository;
import com.example.onboarding.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
 * Tests for the user screens (/users, /users/new, /users/{id}/edit, ...).
 * Sample data: 山田 (4/4 done), 佐藤 (2/4 done), 鈴木 (0/2 done).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
@WithMockUser(roles = "ADMIN")
class UserPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskAssignmentRepository assignmentRepository;

    // --- list ---

    @Test
    void listShowsUsersWithProgress() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attribute("users", hasSize(3)))
                .andExpect(content().string(containsString("山田 太郎")))
                .andExpect(content().string(containsString("4 / 4 完了(100%)")))
                .andExpect(content().string(containsString("2 / 4 完了(50%)")))
                .andExpect(content().string(containsString("0 / 2 完了(0%)")));
    }

    @Test
    void listShowsUnassignedForUserWithoutAssignments() throws Exception {
        User user = new User();
        user.setName("新人 次郎");
        user.setEmail("jiro@example.com");
        userRepository.save(user);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("未割り当て")));
    }

    // --- filter ---

    @Test
    void listCanBeFilteredByDepartment() throws Exception {
        mockMvc.perform(get("/users").param("department", "開発部"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("users", contains(
                        hasProperty("name", is("山田 太郎")),
                        hasProperty("name", is("鈴木 一郎")))))
                .andExpect(content().string(containsString("3 人中 2 人を表示しています。")));
    }

    @Test
    void listCanBeFilteredByIncompleteProgress() throws Exception {
        mockMvc.perform(get("/users").param("status", "INCOMPLETE"))
                .andExpect(model().attribute("users", contains(
                        hasProperty("name", is("佐藤 花子")),
                        hasProperty("name", is("鈴木 一郎")))));
    }

    @Test
    void listCanBeFilteredByOverdue() throws Exception {
        // Fixed clock: 2026-10-15. 佐藤 has two tasks due 2026-04-10 that are not done.
        mockMvc.perform(get("/users").param("status", "OVERDUE"))
                .andExpect(model().attribute("users", contains(hasProperty("name", is("佐藤 花子")))))
                .andExpect(content().string(containsString("期限切れ 2件")));
    }

    @Test
    void listCanBeFilteredByCompleted() throws Exception {
        mockMvc.perform(get("/users").param("status", "COMPLETED"))
                .andExpect(model().attribute("users", contains(hasProperty("name", is("山田 太郎")))));
    }

    @Test
    void listCanBeFilteredByUnassigned() throws Exception {
        mockMvc.perform(get("/users").param("status", "UNASSIGNED"))
                .andExpect(model().attribute("users", hasSize(0)))
                .andExpect(content().string(containsString("条件に一致するユーザーはいません。")));

        User user = new User();
        user.setName("新人 次郎");
        user.setEmail("jiro@example.com");
        userRepository.save(user);

        mockMvc.perform(get("/users").param("status", "UNASSIGNED"))
                .andExpect(model().attribute("users", contains(hasProperty("name", is("新人 次郎")))));
    }

    @Test
    void listCanBeFilteredByDepartmentAndProgress() throws Exception {
        mockMvc.perform(get("/users").param("department", "開発部").param("status", "INCOMPLETE"))
                .andExpect(model().attribute("users", contains(hasProperty("name", is("鈴木 一郎")))))
                .andExpect(model().attribute("selectedDepartment", "開発部"));
    }

    @Test
    void emptyConditionsShowEveryone() throws Exception {
        mockMvc.perform(get("/users").param("department", "").param("status", ""))
                .andExpect(model().attribute("users", hasSize(3)))
                .andExpect(model().attribute("filtered", false))
                .andExpect(content().string(not(containsString("人を表示しています"))));
    }

    @Test
    void unknownProgressConditionReturns400() throws Exception {
        mockMvc.perform(get("/users").param("status", "SOMETHING"))
                .andExpect(status().isBadRequest());
    }

    // --- create ---

    @Test
    void newFormIsShown() throws Exception {
        mockMvc.perform(get("/users/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("user-form"))
                .andExpect(content().string(containsString("ユーザー登録")));
    }

    @Test
    void createSavesUserAndRedirectsToDetail() throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .param("name", "  高橋 美咲  ")
                        .param("email", " takahashi@example.com ")
                        .param("department", "人事部")
                        .param("joinedOn", "2026-11-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "ユーザーを登録しました。"))
                .andReturn();

        User created = userRepository.findAll().stream()
                .filter(u -> "takahashi@example.com".equals(u.getEmail()))
                .findFirst().orElseThrow();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/users/" + created.getId());
        assertThat(created.getName()).isEqualTo("高橋 美咲");
        assertThat(created.getDepartment()).isEqualTo("人事部");
        assertThat(created.getJoinedOn()).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void createShowsErrorsForBlankNameAndInvalidEmail() throws Exception {
        mockMvc.perform(post("/users")
                        .param("name", " ")
                        .param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("user-form"))
                .andExpect(model().attributeHasFieldErrors("userForm", "name", "email"))
                .andExpect(content().string(containsString("氏名を入力してください")))
                .andExpect(content().string(containsString("メールアドレスの形式が正しくありません")));

        assertThat(userRepository.count()).isEqualTo(3);
    }

    @Test
    void createShowsErrorForDuplicateEmailIgnoringCase() throws Exception {
        mockMvc.perform(post("/users")
                        .param("name", "山田 二郎")
                        .param("email", "YAMADA@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("user-form"))
                .andExpect(content().string(containsString("このメールアドレスは既に登録されています")));

        assertThat(userRepository.count()).isEqualTo(3);
    }

    // --- edit ---

    @Test
    void editFormShowsCurrentValues() throws Exception {
        mockMvc.perform(get("/users/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("user-form"))
                .andExpect(content().string(containsString("ユーザー編集")))
                .andExpect(content().string(containsString("value=\"yamada@example.com\"")))
                .andExpect(content().string(containsString("value=\"2026-04-01\"")));
    }

    @Test
    void updateChangesUserAndKeepsOwnEmail() throws Exception {
        mockMvc.perform(post("/users/1")
                        .param("name", "山田 太郎(更新)")
                        .param("email", "yamada@example.com")
                        .param("department", "")
                        .param("joinedOn", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/1"))
                .andExpect(flash().attribute("message", "ユーザーを更新しました。"));

        User user = userRepository.findById(1L).orElseThrow();
        assertThat(user.getName()).isEqualTo("山田 太郎(更新)");
        assertThat(user.getDepartment()).isNull();
        assertThat(user.getJoinedOn()).isNull();
    }

    @Test
    void updateShowsErrorWhenEmailBelongsToAnotherUser() throws Exception {
        mockMvc.perform(post("/users/1")
                        .param("name", "山田 太郎")
                        .param("email", "sato@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("user-form"))
                .andExpect(model().attribute("userId", 1L))
                .andExpect(content().string(containsString("このメールアドレスは既に登録されています")));

        assertThat(userRepository.findById(1L).orElseThrow().getEmail()).isEqualTo("yamada@example.com");
    }

    @Test
    void editAndUpdateReturn404WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/users/9999/edit")).andExpect(status().isNotFound());
        mockMvc.perform(post("/users/9999").param("name", "x").param("email", "x@example.com"))
                .andExpect(status().isNotFound());
    }

    // --- delete ---

    @Test
    void deleteRemovesUserAndTheirAssignments() throws Exception {
        assertThat(assignmentRepository.findByUserIdOrderByTaskIdAsc(2L)).hasSize(4);

        mockMvc.perform(post("/users/2/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("message", "ユーザーを削除しました。"));

        assertThat(userRepository.existsById(2L)).isFalse();
        assertThat(assignmentRepository.findByUserIdOrderByTaskIdAsc(2L)).isEmpty();
    }

    @Test
    void deleteReturns404WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(post("/users/9999/delete"))
                .andExpect(status().isNotFound());
    }
}
