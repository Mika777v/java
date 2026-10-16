package com.example.onboarding.security;

import com.example.onboarding.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

/**
 * How new employees log in (app.member-login):
 * <ul>
 *   <li>{@code link} (default): with a personal link issued by an administrator. No password needed</li>
 *   <li>{@code password}: with email address and password, like administrators</li>
 * </ul>
 * Administrators always log in with a password. Also used by templates as ${@loginSettings...}.
 */
@Component("loginSettings")
public class LoginSettings {

    private final boolean linkMode;
    private final Clock clock;

    public LoginSettings(@Value("${app.member-login:link}") String memberLogin, Clock clock) {
        this.linkMode = switch (memberLogin.strip().toLowerCase(Locale.ROOT)) {
            case "link" -> true;
            case "password" -> false;
            default -> throw new IllegalStateException(
                    "app.member-login (APP_MEMBER_LOGIN) must be 'link' or 'password', but was: " + memberLogin);
        };
        this.clock = clock;
    }

    /** True when new employees log in with personal links. */
    public boolean isLinkMode() {
        return linkMode;
    }

    /** True when the user has a personal link that can be used now. */
    public boolean hasValidLink(User user) {
        return user.hasValidAccessLink(Instant.now(clock));
    }

    /** Whether the user can log in now (for the "(ログイン不可)" label). */
    public boolean canLogIn(User user) {
        if (user.isAdmin() || !linkMode) {
            return user.isLoginEnabled();
        }
        return hasValidLink(user);
    }
}
