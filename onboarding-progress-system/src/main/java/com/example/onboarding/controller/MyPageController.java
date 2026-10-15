package com.example.onboarding.controller;

import com.example.onboarding.entity.AssignmentStatus;
import com.example.onboarding.entity.TaskAssignment;
import com.example.onboarding.entity.User;
import com.example.onboarding.repository.UserRepository;
import com.example.onboarding.security.AppUserDetails;
import com.example.onboarding.service.AssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * "マイページ": the logged-in user's own tasks. Users can change the status of their own tasks only.
 */
@Controller
public class MyPageController {

    private final UserRepository userRepository;
    private final AssignmentService assignmentService;

    public MyPageController(UserRepository userRepository, AssignmentService assignmentService) {
        this.userRepository = userRepository;
        this.assignmentService = assignmentService;
    }

    @GetMapping("/me")
    public String myPage(@AuthenticationPrincipal AppUserDetails me, Model model) {
        User user = currentUser(me);
        List<TaskAssignment> assignments = assignmentService.findAssignments(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("assignments", assignments);
        model.addAttribute("progress", assignmentService.summarize(assignments));
        model.addAttribute("statuses", AssignmentStatus.values());
        return "my-page";
    }

    @PostMapping("/me/assignments/{assignmentId}/status")
    public String changeStatus(@AuthenticationPrincipal AppUserDetails me,
                               @PathVariable Long assignmentId,
                               @RequestParam AssignmentStatus status,
                               RedirectAttributes redirectAttributes) {
        User user = currentUser(me);
        // Only assignments of the logged-in user can be found here; others are "not found"
        TaskAssignment assignment = assignmentService.findAssignment(user.getId(), assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        assignmentService.changeStatus(assignment, status);

        redirectAttributes.addFlashAttribute("message",
                "「" + assignment.getTask().getTitle() + "」を「" + status.getLabel() + "」にしました。");
        return "redirect:/me";
    }

    private User currentUser(AppUserDetails me) {
        if (me == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return userRepository.findById(me.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
}
