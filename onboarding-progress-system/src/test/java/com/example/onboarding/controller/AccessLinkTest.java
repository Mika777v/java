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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

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
 * Personal links for new employees (app.member-login=link, the default).
 * 鈴木 (user 3) is the new employee; an administrator issues and revokes his link.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
class AccessLinkTest {

    private static final long SUZUKI = 3L;
    private static final String LINK_PATTERN = "http://localhost/join/[A-Za-z0-9_-]{43}";

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
        admin.setPasswordHash(passwordEncoder.encode("admin-password"));
        admin = userRepository.save(admin);
    }

    /** Issues a link for the user as the administrator and returns the full URL shown once. */
    private String issueLink(long userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/users/{id}/access-link", userId).with(user(new AppUserDetails(admin))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/" + userId))
                .andExpect(flash().attribute("message", "専用URLを発行しました。"))
                .andReturn();
        return (String) result.getFlashMap().get("accessLink");
    }

    private String tokenOf(String link) {
        return link.substring(link.lastIndexOf('/') + 1);
    }

    private User suzuki() {
        return userRepository.findById(SUZUKI).orElseThrow();
    }

    // --- issuing ---

    @Test
    void issuedLinkIsLongAndRandomAndOnlyItsHashIsSaved() throws Exception {
        String link = issueLink(SUZUKI);

        assertThat(link).matches(LINK_PATTERN);
        User user = suzuki();
        assertThat(user.getAccessTokenHash()).hasSize(64).isNotEqualTo(tokenOf(link));
        assertThat(user.getAccessTokenExpiresAt()).isEqualTo(TestClockConfiguration.NOW.plus(Duration.ofDays(60)));
        assertThat(issueLink(SUZUKI)).isNotEqualTo(link);
    }

    @Test
    void linkIsShownOnlyRightAfterIssuing() throws Exception {
        String link = issueLink(SUZUKI);

        mockMvc.perform(get("/users/3").with(user(new AppUserDetails(admin))).flashAttr("accessLink", link))
                .andExpect(content().string(containsString(link)))
                .andExpect(content().string(containsString("この URL は今だけ表示されます。")));
        mockMvc.perform(get("/users/3").with(user(new AppUserDetails(admin))))
                .andExpect(content().string(not(containsString(tokenOf(link)))))
                .andExpect(content().string(containsString("発行済み(有効期限 2026-12-14 09:00)")))
                .andExpect(content().string(containsString("専用URLを再発行")))
                .andExpect(content().string(containsString("return confirm(&#39;再発行すると、今の URL は使えなくなります。")));
    }

    @Test
    void detailPageOffersLinkForNewEmployeeOnly() throws Exception {
        mockMvc.perform(get("/users/3").with(user(new AppUserDetails(admin))))
                .andExpect(content().string(containsString("まだ発行していません。")))
                .andExpect(content().string(containsString("専用URLを発行")));
        mockMvc.perform(get("/users/{id}", admin.getId()).with(user(new AppUserDetails(admin))))
                .andExpect(content().string(not(containsString("専用URL"))));
    }

    @Test
    void administratorCannotGetLink() throws Exception {
        mockMvc.perform(post("/users/{id}/access-link", admin.getId()).with(user(new AppUserDetails(admin))))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "管理者には専用URLを発行できません(管理者はパスワードでログインします)。"));

        assertThat(userRepository.findById(admin.getId()).orElseThrow().hasAccessLink()).isFalse();
    }

    @Test
    void newEmployeeCannotIssueLinks() throws Exception {
        issueLink(SUZUKI);
        mockMvc.perform(post("/users/2/access-link").with(user(new AppUserDetails(suzuki()))))
                .andExpect(status().isForbidden());
    }

    // --- opening the link ---

    @Test
    void validLinkLogsInAndOpensMyPage() throws Exception {
        String token = tokenOf(issueLink(SUZUKI));

        MvcResult result = mockMvc.perform(get("/join/{token}", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/me"))
                .andExpect(authenticated().withUsername("suzuki@example.com").withRoles("MEMBER"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/me").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("鈴木 一郎 さんのタスク")));
        mockMvc.perform(get("/tasks").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownLinkIsRejected() throws Exception {
        mockMvc.perform(get("/join/{token}", "a".repeat(43)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("このリンクは使えません")))
                .andExpect(unauthenticated());
    }

    @Test
    void expiredLinkIsRejected() throws Exception {
        String token = tokenOf(issueLink(SUZUKI));
        User user = suzuki();
        user.setAccessLink(user.getAccessTokenHash(), TestClockConfiguration.NOW.minusSeconds(1));
        userRepository.save(user);

        mockMvc.perform(get("/join/{token}", token))
                .andExpect(status().isNotFound())
                .andExpect(unauthenticated());
        mockMvc.perform(get("/users/3").with(user(new AppUserDetails(admin))))
                .andExpect(content().string(containsString("期限切れ")));
    }

    @Test
    void reissuingMakesOldLinkUnusable() throws Exception {
        String oldToken = tokenOf(issueLink(SUZUKI));
        String newToken = tokenOf(issueLink(SUZUKI));

        mockMvc.perform(get("/join/{token}", oldToken)).andExpect(status().isNotFound());
        mockMvc.perform(get("/join/{token}", newToken)).andExpect(status().is3xxRedirection());
    }

    @Test
    void revokedLinkIsRejected() throws Exception {
        String token = tokenOf(issueLink(SUZUKI));

        mockMvc.perform(post("/users/3/access-link/delete").with(user(new AppUserDetails(admin))))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "専用URLを無効にしました。"));

        assertThat(suzuki().hasAccessLink()).isFalse();
        mockMvc.perform(get("/join/{token}", token)).andExpect(status().isNotFound());
    }

    @Test
    void makingUserAnAdministratorRemovesLink() throws Exception {
        String token = tokenOf(issueLink(SUZUKI));

        mockMvc.perform(post("/users/3").with(user(new AppUserDetails(admin)))
                        .param("name", "鈴木 一郎")
                        .param("email", "suzuki@example.com")
                        .param("role", "ADMIN")
                        .param("password", "suzuki-password"))
                .andExpect(status().is3xxRedirection());

        assertThat(suzuki().hasAccessLink()).isFalse();
        mockMvc.perform(get("/join/{token}", token)).andExpect(status().isNotFound());
    }

    // --- passwords in this mode ---

    @Test
    void newEmployeeCannotLogInWithPassword() throws Exception {
        User sato = userRepository.findById(2L).orElseThrow();
        sato.setPasswordHash(passwordEncoder.encode("sato-password"));
        userRepository.save(sato);

        mockMvc.perform(formLogin().user("sato@example.com").password("sato-password"))
                .andExpect(unauthenticated());
        mockMvc.perform(formLogin().user("admin@example.com").password("admin-password"))
                .andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    void newEmployeeHasNoPasswordPage() throws Exception {
        issueLink(SUZUKI);
        AppUserDetails suzuki = new AppUserDetails(suzuki());

        mockMvc.perform(get("/me/password").with(user(suzuki)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/me/password").with(user(suzuki))
                        .param("currentPassword", "x").param("newPassword", "new-password-1")
                        .param("confirmPassword", "new-password-1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/me").with(user(suzuki)))
                .andExpect(content().string(not(containsString("href=\"/me/password\""))));

        mockMvc.perform(get("/me/password").with(user(new AppUserDetails(admin))))
                .andExpect(status().isOk());
    }

    @Test
    void loginPageTellsNewEmployeesToUseTheirLink() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(content().string(containsString("管理者から受け取った専用URLを開いてください")));
    }

    @Test
    void userListShowsWhoCannotLogInYet() throws Exception {
        // Nobody but the administrator can log in yet: the three sample users are marked
        String before = mockMvc.perform(get("/users").with(user(new AppUserDetails(admin))))
                .andReturn().getResponse().getContentAsString();
        issueLink(SUZUKI);
        String after = mockMvc.perform(get("/users").with(user(new AppUserDetails(admin))))
                .andReturn().getResponse().getContentAsString();

        assertThat(countOf(before, "(ログイン不可)")).isEqualTo(3);
        assertThat(countOf(after, "(ログイン不可)")).isEqualTo(2);
    }

    private static int countOf(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }
}
