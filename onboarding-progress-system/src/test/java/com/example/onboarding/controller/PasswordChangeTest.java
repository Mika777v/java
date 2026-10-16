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
import org.springframework.test.web.servlet.ResultActions;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Changing one's own password (/me/password).
 * 佐藤 (user 2, new employee) and an administrator get a password before each test.
 */
// New employees log in with a password here (with personal links they have no password)
@SpringBootTest(properties = "app.member-login=password")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
class PasswordChangeTest {

    private static final String SATO_PASSWORD = "sato-password";
    private static final String ADMIN_PASSWORD = "admin-password";
    private static final String NEW_PASSWORD = "brand-new-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User sato;
    private User admin;

    @BeforeEach
    void setUpAccounts() {
        sato = userRepository.findById(2L).orElseThrow();
        sato.setPasswordHash(passwordEncoder.encode(SATO_PASSWORD));
        sato = userRepository.save(sato);

        admin = new User();
        admin.setName("管理者");
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin = userRepository.save(admin);
    }

    private ResultActions change(User who, String current, String newPassword, String confirm) throws Exception {
        return mockMvc.perform(post("/me/password")
                .with(user(new AppUserDetails(who)))
                .param("currentPassword", current)
                .param("newPassword", newPassword)
                .param("confirmPassword", confirm));
    }

    private boolean passwordIs(User who, String password) {
        String hash = userRepository.findById(who.getId()).orElseThrow().getPasswordHash();
        return passwordEncoder.matches(password, hash);
    }

    // --- screen ---

    @Test
    void formIsShownToNewEmployee() throws Exception {
        mockMvc.perform(get("/me/password").with(user(new AppUserDetails(sato))))
                .andExpect(status().isOk())
                .andExpect(view().name("password-form"))
                .andExpect(content().string(containsString("現在のパスワード")))
                .andExpect(content().string(containsString("新しいパスワード(確認)")));
    }

    @Test
    void menuHasLinkToPasswordChange() throws Exception {
        mockMvc.perform(get("/me").with(user(new AppUserDetails(sato))))
                .andExpect(content().string(containsString("href=\"/me/password\"")));
        mockMvc.perform(get("/").with(user(new AppUserDetails(admin))))
                .andExpect(content().string(containsString("href=\"/me/password\"")));
    }

    // --- success ---

    @Test
    void newEmployeeCanChangePasswordAndLogInWithIt() throws Exception {
        change(sato, SATO_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/me"))
                .andExpect(flash().attribute("message", "パスワードを変更しました。"));

        assertThat(passwordIs(sato, NEW_PASSWORD)).isTrue();
        mockMvc.perform(formLogin().user("sato@example.com").password(NEW_PASSWORD))
                .andExpect(authenticated());
        mockMvc.perform(formLogin().user("sato@example.com").password(SATO_PASSWORD))
                .andExpect(unauthenticated());
    }

    @Test
    void administratorCanChangePasswordToo() throws Exception {
        change(admin, ADMIN_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("message", "パスワードを変更しました。"));

        assertThat(passwordIs(admin, NEW_PASSWORD)).isTrue();
    }

    // --- input errors (the password stays as it was) ---

    @Test
    void wrongCurrentPasswordIsRejected() throws Exception {
        change(sato, "wrong-password", NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(view().name("password-form"))
                .andExpect(model().attributeHasFieldErrors("passwordForm", "currentPassword"))
                .andExpect(content().string(containsString("現在のパスワードが正しくありません")));

        assertThat(passwordIs(sato, SATO_PASSWORD)).isTrue();
    }

    @Test
    void confirmationMustMatch() throws Exception {
        change(sato, SATO_PASSWORD, NEW_PASSWORD, NEW_PASSWORD + "x")
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("passwordForm", "confirmPassword"))
                .andExpect(content().string(containsString("新しいパスワードと確認用のパスワードが一致しません")));

        assertThat(passwordIs(sato, SATO_PASSWORD)).isTrue();
    }

    @Test
    void tooShortPasswordIsRejected() throws Exception {
        change(sato, SATO_PASSWORD, "short", "short")
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("passwordForm", "newPassword"))
                .andExpect(content().string(containsString("パスワードは8文字以上72文字以内で入力してください")));

        assertThat(passwordIs(sato, SATO_PASSWORD)).isTrue();
    }

    @Test
    void tooLongPasswordIsRejected() throws Exception {
        String tooLong = "a".repeat(73);
        change(sato, SATO_PASSWORD, tooLong, tooLong)
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("passwordForm", "newPassword"));

        assertThat(passwordIs(sato, SATO_PASSWORD)).isTrue();
    }

    @Test
    void newPasswordMustDifferFromCurrent() throws Exception {
        change(sato, SATO_PASSWORD, SATO_PASSWORD, SATO_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("passwordForm", "newPassword"))
                .andExpect(content().string(containsString("現在のパスワードと違うパスワードを入力してください")));
    }

    @Test
    void blankFieldsAreRejected() throws Exception {
        change(sato, "", "", "")
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("passwordForm",
                        "currentPassword", "newPassword", "confirmPassword"))
                .andExpect(content().string(containsString("現在のパスワードを入力してください")));

        assertThat(passwordIs(sato, SATO_PASSWORD)).isTrue();
    }

    @Test
    void enteredPasswordsAreNotShownAgain() throws Exception {
        change(sato, "wrong-password", NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(content().string(not(containsString("wrong-password"))))
                .andExpect(content().string(not(containsString(NEW_PASSWORD))));
    }

    // --- access ---

    @Test
    void notLoggedInIsSentToLogin() throws Exception {
        mockMvc.perform(get("/me/password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(post("/me/password")
                        .param("currentPassword", SATO_PASSWORD)
                        .param("newPassword", NEW_PASSWORD)
                        .param("confirmPassword", NEW_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void changeWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/me/password")
                        .with(user(new AppUserDetails(sato)))
                        .param("currentPassword", SATO_PASSWORD)
                        .param("newPassword", NEW_PASSWORD)
                        .param("confirmPassword", NEW_PASSWORD))
                .andExpect(status().isForbidden());

        assertThat(passwordIs(sato, SATO_PASSWORD)).isTrue();
    }
}
