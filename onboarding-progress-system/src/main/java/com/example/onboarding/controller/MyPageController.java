package com.example.onboarding.controller;

import com.example.onboarding.entity.AssignmentStatus;
import com.example.onboarding.entity.TaskAssignment;
import com.example.onboarding.entity.User;
import com.example.onboarding.form.PasswordChangeForm;
import com.example.onboarding.repository.UserRepository;
import com.example.onboarding.security.AppUserDetails;
import com.example.onboarding.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * "マイページ": the logged-in user's own tasks. Users can change the status of their own tasks only,
 * and can change their own password (/me/password).
 */
@Controller
public class MyPageController {

    private final UserRepository userRepository;
    private final AssignmentService assignmentService;
    private final PasswordEncoder passwordEncoder;

    public MyPageController(UserRepository userRepository,
                            AssignmentService assignmentService,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.assignmentService = assignmentService;
        this.passwordEncoder = passwordEncoder;
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

    @GetMapping("/me/password")
    public String passwordForm(Model model) {
        model.addAttribute("passwordForm", new PasswordChangeForm());
        return "password-form";
    }

    @PostMapping("/me/password")
    public String changePassword(@AuthenticationPrincipal AppUserDetails me,
                                 @Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        User user = currentUser(me);
        // The current password is asked again, so that someone else cannot change it on a computer left logged in
        if (!bindingResult.hasFieldErrors("currentPassword")
                && !passwordEncoder.matches(form.getCurrentPassword(), user.getPasswordHash())) {
            bindingResult.rejectValue("currentPassword", "wrong", "現在のパスワードが正しくありません");
        }
        if (!bindingResult.hasFieldErrors("newPassword") && !bindingResult.hasFieldErrors("confirmPassword")
                && !form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "新しいパスワードと確認用のパスワードが一致しません");
        }
        if (!bindingResult.hasFieldErrors("newPassword")
                && form.getNewPassword().equals(form.getCurrentPassword())) {
            bindingResult.rejectValue("newPassword", "same", "現在のパスワードと違うパスワードを入力してください");
        }
        if (bindingResult.hasErrors()) {
            form.clear();
            return "password-form";
        }

        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("message", "パスワードを変更しました。");
        return user.isAdmin() ? "redirect:/" : "redirect:/me";
    }

    private User currentUser(AppUserDetails me) {
        if (me == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return userRepository.findById(me.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
}
