package com.example.onboarding.controller;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import com.example.onboarding.entity.AssignmentStatus;
import com.example.onboarding.entity.Role;
import com.example.onboarding.entity.TaskAssignment;
import com.example.onboarding.entity.User;
import com.example.onboarding.repository.TaskAssignmentRepository;
import com.example.onboarding.repository.UserRepository;
import com.example.onboarding.security.AppUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.example.onboarding.WebTestSupport.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Login and permissions.
 * In each test: an administrator (admin@example.com) and 佐藤 (user 2, new employee) can log in;
 * 鈴木 (user 3) has no password.
 */
// New employees log in with a password here; personal links are tested in AccessLinkTest
@SpringBootTest(properties = "app.member-login=password")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
class SecurityTest {

    private static final String ADMIN_PASSWORD = "admin-password";
    private static final String SATO_PASSWORD = "sato-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskAssignmentRepository assignmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User sato;

    @BeforeEach
    void setUpAccounts() {
        admin = new User();
        admin.setName("管理者");
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin = userRepository.save(admin);

        sato = userRepository.findById(2L).orElseThrow();
        sato.setPasswordHash(passwordEncoder.encode(SATO_PASSWORD));
        sato = userRepository.save(sato);
    }

    // --- not logged in ---

    @Test
    void pagesRedirectToLoginWhenNotLoggedIn() throws Exception {
        for (String path : new String[] {"/", "/tasks", "/users", "/me"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
    }

    @Test
    void loginPageAndStylesAreOpenToEveryone() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ログイン")));
        mockMvc.perform(get("/css/app.css")).andExpect(status().isOk());
    }

    @Test
    void apiRequiresAdministrator() throws Exception {
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tasks").with(httpBasic("sato@example.com", SATO_PASSWORD)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tasks").with(httpBasic("admin@example.com", ADMIN_PASSWORD)))
                .andExpect(status().isOk());
    }

    // --- logging in and out ---

    @Test
    void administratorCanLogIn() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin@example.com").password(ADMIN_PASSWORD))
                .andExpect(authenticated().withRoles("ADMIN"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void emailIsNotCaseSensitive() throws Exception {
        mockMvc.perform(formLogin("/login").user("SATO@example.com").password(SATO_PASSWORD))
                .andExpect(authenticated().withRoles("MEMBER"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin@example.com").password("wrong"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void userWithoutPasswordCannotLogIn() throws Exception {
        mockMvc.perform(formLogin("/login").user("suzuki@example.com").password(""))
                .andExpect(unauthenticated());
    }

    @Test
    void logoutReturnsToLoginPage() throws Exception {
        mockMvc.perform(post("/logout").with(user(new AppUserDetails(sato))))
                .andExpect(redirectedUrl("/login?logout"));
    }

    // --- new employee ---

    @Test
    void newEmployeeIsSentFromTopPageToMyPage() throws Exception {
        mockMvc.perform(get("/").with(user(new AppUserDetails(sato))))
                .andExpect(redirectedUrl("/me"));
    }

    @Test
    void myPageShowsOwnTasks() throws Exception {
        mockMvc.perform(get("/me").with(user(new AppUserDetails(sato))))
                .andExpect(status().isOk())
                .andExpect(view().name("my-page"))
                .andExpect(content().string(containsString("佐藤 花子 さんのタスク")))
                .andExpect(content().string(containsString("2 / 4 完了(50%)")))
                .andExpect(content().string(containsString("マイページ")))
                .andExpect(content().string(not(containsString("ダッシュボード"))));
    }

    @Test
    void newEmployeeCannotOpenAdministratorPages() throws Exception {
        for (String path : new String[] {"/tasks", "/tasks/new", "/users", "/users/2", "/users/2/edit"}) {
            mockMvc.perform(get(path).with(user(new AppUserDetails(sato))))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/tasks/1/delete").with(user(new AppUserDetails(sato))))
                .andExpect(status().isForbidden());
    }

    @Test
    void newEmployeeCanChangeStatusOfOwnTask() throws Exception {
        TaskAssignment own = assignmentRepository.findByUserIdOrderByTaskIdAsc(2L).get(3);

        mockMvc.perform(post("/me/assignments/{aid}/status", own.getId())
                        .param("status", "IN_PROGRESS")
                        .with(user(new AppUserDetails(sato))))
                .andExpect(redirectedUrl("/me"))
                .andExpect(flash().attribute("message", "「チームメンバーとの顔合わせ」を「対応中」にしました。"));

        assertThat(assignmentRepository.findById(own.getId()).orElseThrow().getStatus())
                .isEqualTo(AssignmentStatus.IN_PROGRESS);
    }

    @Test
    void newEmployeeCannotChangeSomeoneElsesTask() throws Exception {
        TaskAssignment suzukis = assignmentRepository.findByUserIdOrderByTaskIdAsc(3L).get(0);

        mockMvc.perform(post("/me/assignments/{aid}/status", suzukis.getId())
                        .param("status", "DONE")
                        .with(user(new AppUserDetails(sato))))
                .andExpect(status().isNotFound());

        assertThat(assignmentRepository.findById(suzukis.getId()).orElseThrow().getStatus())
                .isEqualTo(AssignmentStatus.IN_PROGRESS);
    }

    @Test
    void formsWithoutCsrfTokenAreRejected() throws Exception {
        TaskAssignment own = assignmentRepository.findByUserIdOrderByTaskIdAsc(2L).get(3);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/me/assignments/{aid}/status", own.getId())
                        .param("status", "DONE")
                        .with(user(new AppUserDetails(sato))))
                .andExpect(status().isForbidden());
    }

    // --- administrator managing accounts ---

    @Test
    void administratorCanGiveLoginToNewEmployee() throws Exception {
        mockMvc.perform(post("/users")
                        .param("name", "高橋 美咲")
                        .param("email", "takahashi@example.com")
                        .param("role", "MEMBER")
                        .param("password", "first-password")
                        .with(user(new AppUserDetails(admin))))
                .andExpect(status().is3xxRedirection());

        User created = userRepository.findByEmailIgnoreCase("takahashi@example.com").orElseThrow();
        assertThat(created.getRole()).isEqualTo(Role.MEMBER);
        assertThat(created.getPasswordHash()).isNotEqualTo("first-password");
        assertThat(passwordEncoder.matches("first-password", created.getPasswordHash())).isTrue();

        mockMvc.perform(formLogin("/login").user("takahashi@example.com").password("first-password"))
                .andExpect(authenticated().withRoles("MEMBER"));
    }

    @Test
    void shortPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/users")
                        .param("name", "高橋 美咲")
                        .param("email", "takahashi@example.com")
                        .param("role", "MEMBER")
                        .param("password", "short")
                        .with(user(new AppUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("userForm", "password"))
                .andExpect(content().string(containsString("パスワードは8文字以上で入力してください")));
    }

    @Test
    void blankPasswordOnEditKeepsCurrentPassword() throws Exception {
        String before = sato.getPasswordHash();

        mockMvc.perform(post("/users/2")
                        .param("name", "佐藤 花子")
                        .param("email", "sato@example.com")
                        .param("role", "MEMBER")
                        .param("password", "")
                        .with(user(new AppUserDetails(admin))))
                .andExpect(redirectedUrl("/users/2"));

        assertThat(userRepository.findById(2L).orElseThrow().getPasswordHash()).isEqualTo(before);
    }

    @Test
    void administratorCannotTakeAwayOwnAdministratorRole() throws Exception {
        mockMvc.perform(post("/users/{id}", admin.getId())
                        .param("name", "管理者")
                        .param("email", "admin@example.com")
                        .param("role", "MEMBER")
                        .with(user(new AppUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("自分の役割は管理者から変更できません")));

        assertThat(userRepository.findById(admin.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void administratorCannotDeleteThemselves() throws Exception {
        mockMvc.perform(post("/users/{id}/delete", admin.getId()).with(user(new AppUserDetails(admin))))
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("error", "自分自身は削除できません。"));

        assertThat(userRepository.existsById(admin.getId())).isTrue();
    }

    @Test
    void userListShowsRoleAndLoginState() throws Exception {
        mockMvc.perform(get("/users").with(user(new AppUserDetails(admin))))
                .andExpect(content().string(containsString("管理者")))
                .andExpect(content().string(containsString("(ログイン不可)")));
    }

    @Test
    void progressFiltersLeaveOutAdministrators() throws Exception {
        mockMvc.perform(get("/users").param("status", "UNASSIGNED").with(user(new AppUserDetails(admin))))
                .andExpect(content().string(containsString("条件に一致するユーザーはいません。")));
    }

    // --- personal links are not used in this mode ---

    @Test
    void personalLinksAreNotAvailableWithPasswordLogin() throws Exception {
        mockMvc.perform(get("/join/some-token"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/users/3/access-link").with(user(new AppUserDetails(admin))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/users/3").with(user(new AppUserDetails(admin))))
                .andExpect(content().string(not(containsString("専用URL"))));
    }
}
