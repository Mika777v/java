package com.example.onboarding.controller;

import com.example.onboarding.entity.User;
import com.example.onboarding.security.AppUserDetails;
import com.example.onboarding.security.LoginSettings;
import com.example.onboarding.service.AccessLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Login with a personal link (/join/{token}) for new employees.
 * Opening a valid link logs the user in and moves on to /me, so the link does not stay in the address bar.
 */
@Controller
public class JoinController {

    private final AccessLinkService accessLinkService;
    private final LoginSettings loginSettings;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public JoinController(AccessLinkService accessLinkService, LoginSettings loginSettings) {
        this.accessLinkService = accessLinkService;
        this.loginSettings = loginSettings;
    }

    @GetMapping("/join/{token}")
    public String join(@PathVariable String token, HttpServletRequest request, HttpServletResponse response) {
        if (!loginSettings.isLinkMode()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Optional<User> user = accessLinkService.findUser(token);
        if (user.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "join-invalid";
        }

        AppUserDetails details = new AppUserDetails(user.get());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities()));
        // New session id after logging in, like the normal login does (protects against session fixation)
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return "redirect:/me";
    }
}
