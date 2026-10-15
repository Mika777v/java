package com.example.onboarding.controller;

import com.example.onboarding.entity.AssignmentStatus;
import com.example.onboarding.entity.TaskAssignment;
import com.example.onboarding.entity.User;
import com.example.onboarding.form.UserForm;
import com.example.onboarding.repository.UserRepository;
import com.example.onboarding.service.AssignmentException;
import com.example.onboarding.service.AssignmentService;
import com.example.onboarding.service.UserProgress;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class UserPageController {

    private final UserRepository userRepository;
    private final AssignmentService assignmentService;

    public UserPageController(UserRepository userRepository, AssignmentService assignmentService) {
        this.userRepository = userRepository;
        this.assignmentService = assignmentService;
    }

    // --- users ---

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll(Sort.by("id"));
        Map<Long, UserProgress> progress = assignmentService.progressByUser();
        model.addAttribute("users", users);
        model.addAttribute("progress", progress);
        model.addAttribute("noProgress", UserProgress.NONE);
        return "users";
    }

    @GetMapping("/users/new")
    public String newUser(Model model) {
        model.addAttribute("userForm", new UserForm());
        return "user-form";
    }

    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute("userForm") UserForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasFieldErrors("email")
                && userRepository.existsByEmailIgnoreCase(form.normalizedEmail())) {
            rejectDuplicateEmail(bindingResult);
        }
        if (bindingResult.hasErrors()) {
            return "user-form";
        }

        User user = new User();
        form.applyTo(user);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("message", "ユーザーを登録しました。");
        return "redirect:/users/" + user.getId();
    }

    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        User user = findUser(id);
        model.addAttribute("userId", id);
        model.addAttribute("userForm", UserForm.from(user));
        return "user-form";
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id,
                             @Valid @ModelAttribute("userForm") UserForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        User user = findUser(id);
        if (!bindingResult.hasFieldErrors("email")
                && userRepository.existsByEmailIgnoreCaseAndIdNot(form.normalizedEmail(), id)) {
            rejectDuplicateEmail(bindingResult);
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", id);
            return "user-form";
        }

        form.applyTo(user);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("message", "ユーザーを更新しました。");
        return "redirect:/users/" + id;
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = findUser(id);
        userRepository.delete(user);

        redirectAttributes.addFlashAttribute("message", "ユーザーを削除しました。");
        return "redirect:/users";
    }

    // --- user detail and assignments ---

    @GetMapping("/users/{id}")
    public String showUser(@PathVariable Long id, Model model) {
        User user = findUser(id);
        List<TaskAssignment> assignments = assignmentService.findAssignments(id);
        long done = assignments.stream().filter(a -> a.getStatus() == AssignmentStatus.DONE).count();

        model.addAttribute("user", user);
        model.addAttribute("assignments", assignments);
        model.addAttribute("progress", new UserProgress(assignments.size(), done));
        model.addAttribute("assignableTasks", assignmentService.findAssignableTasks(id));
        model.addAttribute("statuses", AssignmentStatus.values());
        return "user-detail";
    }

    @PostMapping("/users/{id}/assignments")
    public String assignTask(@PathVariable Long id,
                             @RequestParam(required = false) Long taskId,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                             RedirectAttributes redirectAttributes) {
        User user = findUser(id);
        try {
            TaskAssignment assignment = assignmentService.assign(user, taskId, dueDate);
            redirectAttributes.addFlashAttribute("message",
                    "「" + assignment.getTask().getTitle() + "」を割り当てました。");
        } catch (AssignmentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users/" + id;
    }

    @PostMapping("/users/{id}/assignments/all")
    public String assignAllActiveTasks(@PathVariable Long id,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                                       RedirectAttributes redirectAttributes) {
        User user = findUser(id);
        int count = assignmentService.assignAllActive(user, dueDate);
        if (count == 0) {
            redirectAttributes.addFlashAttribute("error", "割り当てられる有効なタスクはありません。");
        } else {
            redirectAttributes.addFlashAttribute("message", count + "件のタスクを割り当てました。");
        }
        return "redirect:/users/" + id;
    }

    @PostMapping("/users/{id}/assignments/{assignmentId}/status")
    public String changeStatus(@PathVariable Long id,
                               @PathVariable Long assignmentId,
                               @RequestParam AssignmentStatus status,
                               RedirectAttributes redirectAttributes) {
        TaskAssignment assignment = findAssignment(id, assignmentId);
        assignmentService.changeStatus(assignment, status);

        redirectAttributes.addFlashAttribute("message",
                "「" + assignment.getTask().getTitle() + "」を「" + status.getLabel() + "」にしました。");
        return "redirect:/users/" + id;
    }

    @PostMapping("/users/{id}/assignments/{assignmentId}/delete")
    public String unassignTask(@PathVariable Long id,
                               @PathVariable Long assignmentId,
                               RedirectAttributes redirectAttributes) {
        TaskAssignment assignment = findAssignment(id, assignmentId);
        String title = assignment.getTask().getTitle();
        assignmentService.unassign(assignment);

        redirectAttributes.addFlashAttribute("message", "「" + title + "」の割り当てを解除しました。");
        return "redirect:/users/" + id;
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /** The assignment must belong to the user in the URL; otherwise it is treated as not found. */
    private TaskAssignment findAssignment(Long userId, Long assignmentId) {
        return assignmentService.findAssignment(userId, assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    private void rejectDuplicateEmail(BindingResult bindingResult) {
        bindingResult.rejectValue("email", "duplicate", "このメールアドレスは既に登録されています");
    }
}
