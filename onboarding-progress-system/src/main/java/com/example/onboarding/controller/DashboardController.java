package com.example.onboarding.controller;

import com.example.onboarding.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Top page: overall progress, users who need attention and tasks due soon.
 */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String dashboard(Authentication authentication, Model model) {
        // The dashboard is for administrators; new employees go to their own page
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!admin) {
            return "redirect:/me";
        }
        model.addAttribute("dashboard", dashboardService.build());
        return "dashboard";
    }
}
