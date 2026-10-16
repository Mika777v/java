package com.example.onboarding.controller;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import com.example.onboarding.entity.Role;
import com.example.onboarding.entity.User;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A password set by an administrator for someone else must be changed at the first login.
 * Runs with password login for new employees, so that both roles can be checked.
 */
@SpringBootTest(properties = "app.member-login=password")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
class FirstLoginPasswordChangeTest {

    private static final String ADMIN_PASSWORD = "admin-password";
    private static final String INITIAL_PASSWORD = "initial-password";
    private static final String OWN_PASSWORD = "my-own-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;

    @BeforeEach
    void setUpAdministrator() {
        admin = new User();
        admin.setName("管理者");
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin = userRepository.save(admin);
    }

    private AppUserDetails asAdmin() {
        return new AppUserDetails(admin);
    }

    /** The administrator registers a second administrator with an initial password. */
    private User createSecondAdministrator() throws Exception {
        mockMvc.perform(post("/users").with(user(asAdmin()))
                        .param("name", "管理者 二郎")
                        .param("email", "admin2@example.com")
                        .param("role", "ADMIN")
                        .param("password", INITIAL_PASSWORD))
                .andExpect(status().is3xxRedirection());
        return userRepository.findByEmailIgnoreCase("admin2@example.com").orElseThrow();
    }

    private User reload(User user) {
        return userRepository.findById(user.getId()).orElseThrow();
    }

    // --- when the change becomes required ---

    @Test
    void passwordSetByAdministratorMustBeChanged() throws Exception {
        assertThat(createSecondAdministrator().isPasswordChangeRequired()).isTrue();

        mockMvc.perform(post("/users/2").with(user(asAdmin()))
                        .param("name", "佐藤 花子")
                        .param("email", "sato@example.com")
                        .param("role", "MEMBER")
                        .param("password", INITIAL_PASSWORD))
                .andExpect(status().is3xxRedirection());
        assertThat(reload(userRepository.findById(2L).orElseThrow()).isPasswordChangeRequired()).isTrue();
    }

    @Test
    void ownPasswordSetOnUserFormIsNotRequiredToChange() throws Exception {
        mockMvc.perform(post("/users/{id}", admin.getId()).with(user(asAdmin()))
                        .param("name", "管理者")
                        .param("email", "admin@example.com")
                        .param("role", "ADMIN")
                        .param("password", "new-admin-password"))
                .andExpect(status().is3xxRedirection());

        assertThat(reload(admin).isPasswordChangeRequired()).isFalse();
    }

    @Test
    void editingWithoutNewPasswordKeepsFlag() throws Exception {
        User second = createSecondAdministrator();
        mockMvc.perform(post("/users/{id}", second.getId()).with(user(asAdmin()))
                        .param("name", "管理者 二郎(更新)")
                        .param("email", "admin2@example.com")
                        .param("role", "ADMIN")
                        .param("password", ""))
                .andExpect(status().is3xxRedirection());

        assertThat(reload(second).isPasswordChangeRequired()).isTrue();
    }

    // --- what the user can do until the password is changed ---

    @Test
    void pagesAreRedirectedToPasswordChange() throws Exception {
        AppUserDetails second = new AppUserDetails(createSecondAdministrator());

        for (String path : new String[] {"/", "/tasks", "/users", "/me"}) {
            mockMvc.perform(get(path).with(user(second)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/me/password"));
        }
        mockMvc.perform(post("/tasks").with(user(second)).param("title", "Should not be created"))
                .andExpect(redirectedUrl("/me/password"));
    }

    @Test
    void apiIsForbiddenUntilPasswordIsChanged() throws Exception {
        createSecondAdministrator();

        mockMvc.perform(get("/api/tasks").with(httpBasic("admin2@example.com", INITIAL_PASSWORD)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tasks").with(httpBasic("admin@example.com", ADMIN_PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void passwordPageExplainsWhyAndHasNoWayBack() throws Exception {
        AppUserDetails second = new AppUserDetails(createSecondAdministrator());

        mockMvc.perform(get("/me/password").with(user(second)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("管理者が設定したパスワードでログインしています。")))
                .andExpect(content().string(not(containsString(">戻る</a>"))));
        mockMvc.perform(get("/me/password").with(user(asAdmin())))
                .andExpect(content().string(not(containsString("管理者が設定したパスワードでログインしています。"))))
                .andExpect(content().string(containsString(">戻る</a>")));
    }

    @Test
    void changingPasswordUnlocksPages() throws Exception {
        User second = createSecondAdministrator();
        AppUserDetails me = new AppUserDetails(second);

        mockMvc.perform(post("/me/password").with(user(me))
                        .param("currentPassword", INITIAL_PASSWORD)
                        .param("newPassword", OWN_PASSWORD)
                        .param("confirmPassword", OWN_PASSWORD))
                .andExpect(redirectedUrl("/"));

        assertThat(reload(second).isPasswordChangeRequired()).isFalse();
        mockMvc.perform(get("/").with(user(me))).andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks").with(httpBasic("admin2@example.com", OWN_PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void failedChangeKeepsTheRequirement() throws Exception {
        User second = createSecondAdministrator();

        mockMvc.perform(post("/me/password").with(user(new AppUserDetails(second)))
                        .param("currentPassword", "wrong-password")
                        .param("newPassword", OWN_PASSWORD)
                        .param("confirmPassword", OWN_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("管理者が設定したパスワードでログインしています。")));

        assertThat(reload(second).isPasswordChangeRequired()).isTrue();
    }

    @Test
    void newEmployeeWithPasswordLoginMustChangeToo() throws Exception {
        mockMvc.perform(post("/users/3").with(user(asAdmin()))
                        .param("name", "鈴木 一郎")
                        .param("email", "suzuki@example.com")
                        .param("role", "MEMBER")
                        .param("password", INITIAL_PASSWORD))
                .andExpect(status().is3xxRedirection());
        AppUserDetails suzuki = new AppUserDetails(userRepository.findById(3L).orElseThrow());

        mockMvc.perform(get("/me").with(user(suzuki))).andExpect(redirectedUrl("/me/password"));
        mockMvc.perform(post("/me/password").with(user(suzuki))
                        .param("currentPassword", INITIAL_PASSWORD)
                        .param("newPassword", OWN_PASSWORD)
                        .param("confirmPassword", OWN_PASSWORD))
                .andExpect(redirectedUrl("/me"));
        mockMvc.perform(get("/me").with(user(suzuki))).andExpect(status().isOk());
    }

    @Test
    void newEmployeePasswordCanBeResetWithPasswordLogin() throws Exception {
        String temporary = (String) mockMvc.perform(post("/users/3/password-reset").with(user(asAdmin())))
                .andExpect(status().is3xxRedirection())
                .andReturn().getFlashMap().get("temporaryPassword");

        User suzuki = userRepository.findById(3L).orElseThrow();
        assertThat(passwordEncoder.matches(temporary, suzuki.getPasswordHash())).isTrue();
        assertThat(suzuki.isPasswordChangeRequired()).isTrue();
    }
}
