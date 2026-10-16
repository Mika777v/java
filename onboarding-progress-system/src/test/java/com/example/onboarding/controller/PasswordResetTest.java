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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An administrator resets someone's forgotten password: a temporary password is generated,
 * shown once, and must be changed at the next login.
 * New employees log in with personal links here (the default), so only administrators have passwords.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
class PasswordResetTest {

    private static final String OLD_PASSWORD = "forgotten-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User second;

    @BeforeEach
    void setUpAdministrators() {
        admin = saveAdministrator("管理者", "admin@example.com");
        second = saveAdministrator("管理者 二郎", "admin2@example.com");
    }

    private User saveAdministrator(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setRole(Role.ADMIN);
        user.setPasswordHash(passwordEncoder.encode(OLD_PASSWORD));
        return userRepository.save(user);
    }

    private String reset(User target) throws Exception {
        return (String) mockMvc.perform(post("/users/{id}/password-reset", target.getId()).with(user(new AppUserDetails(admin))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/" + target.getId()))
                .andExpect(flash().attribute("message", "仮パスワードを発行しました。"))
                .andReturn().getFlashMap().get("temporaryPassword");
    }

    @Test
    void resetSetsRandomTemporaryPasswordThatMustBeChanged() throws Exception {
        String temporary = reset(second);

        assertThat(temporary).matches("[a-km-zA-HJ-NP-Z2-9]{12}");
        User reloaded = userRepository.findById(second.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(temporary, reloaded.getPasswordHash())).isTrue();
        assertThat(reloaded.isPasswordChangeRequired()).isTrue();
        assertThat(reset(second)).isNotEqualTo(temporary);
    }

    @Test
    void temporaryPasswordWorksAndOldOneDoesNot() throws Exception {
        String temporary = reset(second);

        mockMvc.perform(formLogin().user("admin2@example.com").password(temporary))
                .andExpect(authenticated());
        mockMvc.perform(formLogin().user("admin2@example.com").password(OLD_PASSWORD))
                .andExpect(unauthenticated());
        mockMvc.perform(get("/").with(user(new AppUserDetails(second))))
                .andExpect(redirectedUrl("/me/password"));
    }

    @Test
    void temporaryPasswordIsShownOnlyOnce() throws Exception {
        String temporary = reset(second);

        mockMvc.perform(get("/users/{id}", second.getId()).with(user(new AppUserDetails(admin)))
                        .flashAttr("temporaryPassword", temporary))
                .andExpect(content().string(containsString(temporary)))
                .andExpect(content().string(containsString("仮パスワードは今だけ表示されます。")));
        mockMvc.perform(get("/users/{id}", second.getId()).with(user(new AppUserDetails(admin))))
                .andExpect(content().string(not(containsString(temporary))))
                .andExpect(content().string(containsString("本人のパスワード変更待ちです")))
                .andExpect(content().string(containsString("パスワードをリセット")));
    }

    @Test
    void administratorCannotResetOwnPassword() throws Exception {
        mockMvc.perform(post("/users/{id}/password-reset", admin.getId()).with(user(new AppUserDetails(admin))))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "自分のパスワードは「パスワード変更」画面で変更してください。"));

        User reloaded = userRepository.findById(admin.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(OLD_PASSWORD, reloaded.getPasswordHash())).isTrue();
        mockMvc.perform(get("/users/{id}", admin.getId()).with(user(new AppUserDetails(admin))))
                .andExpect(content().string(not(containsString("パスワードをリセット"))));
    }

    @Test
    void newEmployeesWithPersonalLinksHaveNoPasswordToReset() throws Exception {
        mockMvc.perform(post("/users/3/password-reset").with(user(new AppUserDetails(admin))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/users/3").with(user(new AppUserDetails(admin))))
                .andExpect(content().string(not(containsString("パスワードをリセット"))))
                .andExpect(content().string(not(containsString("仮パスワードを発行"))));
    }

    @Test
    void newEmployeeCannotResetPasswords() throws Exception {
        mockMvc.perform(post("/users/{id}/password-reset", second.getId())
                        .with(user(new AppUserDetails(userRepository.findById(3L).orElseThrow()))))
                .andExpect(status().isForbidden());
    }
}
