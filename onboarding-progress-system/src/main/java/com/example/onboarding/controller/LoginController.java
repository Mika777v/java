package com.example.onboarding.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Login page. The login itself (POST /login) and logout (POST /logout) are handled by Spring Security.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
