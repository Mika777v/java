package com.example.onboarding.controller;

import com.example.onboarding.entity.AssignmentStatus;
import com.example.onboarding.entity.Role;
import com.example.onboarding.entity.TaskAssignment;
import com.example.onboarding.entity.User;
import com.example.onboarding.form.UserForm;
import com.example.onboarding.repository.UserRepository;
import com.example.onboarding.security.AppUserDetails;
import com.example.onboarding.security.LoginSettings;
import com.example.onboarding.security.TemporaryPasswords;
import com.example.onboarding.service.AccessLinkService;
import com.example.onboarding.service.AssignmentException;
import com.example.onboarding.service.AssignmentService;
import com.example.onboarding.service.ChecklistService;
import com.example.onboarding.service.ProgressFilter;
import com.example.onboarding.service.UserProgress;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class UserPageController {

    private final UserRepository userRepository;
    private final AssignmentService assignmentService;
    private final PasswordEncoder passwordEncoder;
    private final AccessLinkService accessLinkService;
    private final LoginSettings loginSettings;
    private final TemporaryPasswords temporaryPasswords;
    private final ChecklistService checklistService;
    private final String baseUrl;

    public UserPageController(UserRepository userRepository,
                              AssignmentService assignmentService,
                              PasswordEncoder passwordEncoder,
                              AccessLinkService accessLinkService,
                              LoginSettings loginSettings,
                              TemporaryPasswords temporaryPasswords,
                              ChecklistService checklistService,
                              @Value("${app.base-url:}") String baseUrl) {
        this.userRepository = userRepository;
        this.assignmentService = assignmentService;
        this.passwordEncoder = passwordEncoder;
        this.accessLinkService = accessLinkService;
        this.loginSettings = loginSettings;
        this.temporaryPasswords = temporaryPasswords;
        this.checklistService = checklistService;
        this.baseUrl = baseUrl;
    }

    // --- users ---

    /**
     * User list. Can be narrowed down by department and by progress, e.g. /users?department=開発部&status=OVERDUE
     */
    @GetMapping("/users")
    public String listUsers(@RequestParam(required = false) String department,
                            @RequestParam(required = false) ProgressFilter status,
                            Model model) {
        List<User> allUsers = userRepository.findAll(Sort.by("id"));
        Map<Long, UserProgress> progress = assignmentService.progressByUser();
        String selectedDepartment = department == null || department.isBlank() ? null : department;

        List<User> users = allUsers.stream()
                .filter(user -> selectedDepartment == null || selectedDepartment.equals(user.getDepartment()))
                // Progress conditions are about new employees, so administrators are left out
                .filter(user -> status == null
                        || (!user.isAdmin() && status.matches(progress.getOrDefault(user.getId(), UserProgress.NONE))))
                .toList();
        List<String> departments = allUsers.stream()
                .map(User::getDepartment)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        model.addAttribute("users", users);
        model.addAttribute("totalUserCount", allUsers.size());
        model.addAttribute("progress", progress);
        model.addAttribute("noProgress", UserProgress.NONE);
        model.addAttribute("departments", departments);
        model.addAttribute("filters", ProgressFilter.values());
        model.addAttribute("selectedDepartment", selectedDepartment);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("filtered", selectedDepartment != null || status != null);
        return "users";
    }

    @GetMapping("/users/new")
    public String newUser(Model model) {
        model.addAttribute("userForm", new UserForm());
        return "user-form";
    }

    /** Choices for the role select on the user form. */
    @ModelAttribute("roles")
    public Role[] roles() {
        return Role.values();
    }

    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute("userForm") UserForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasFieldErrors("email")
                && userRepository.existsByEmailIgnoreCase(form.normalizedEmail())) {
            rejectDuplicateEmail(bindingResult);
        }
        checkPasswordLength(form, bindingResult);
        if (bindingResult.hasErrors()) {
            form.setPassword(null);
            return "user-form";
        }

        User user = new User();
        form.applyTo(user);
        applyPassword(form, user, true);
        if (!saveUnlessEmailIsTaken(user, bindingResult)) {
            form.setPassword(null);
            return "user-form";
        }

        redirectAttributes.addFlashAttribute("message", "ユーザーを登録しました。");
        return "redirect:/users/" + user.getId();
    }

    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        User user = findUser(id);
        model.addAttribute("userId", id);
        model.addAttribute("loginEnabled", user.isLoginEnabled());
        model.addAttribute("userForm", UserForm.from(user));
        return "user-form";
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id,
                             @Valid @ModelAttribute("userForm") UserForm form,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal AppUserDetails me,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        User user = findUser(id);
        if (!bindingResult.hasFieldErrors("email")
                && userRepository.existsByEmailIgnoreCaseAndIdNot(form.normalizedEmail(), id)) {
            rejectDuplicateEmail(bindingResult);
        }
        checkPasswordLength(form, bindingResult);
        // An administrator cannot take away their own administrator role (they would lock themselves out)
        if (isSelf(me, id) && form.getRole() != Role.ADMIN) {
            bindingResult.rejectValue("role", "self", "自分の役割は管理者から変更できません");
        }
        if (bindingResult.hasErrors()) {
            form.setPassword(null);
            model.addAttribute("userId", id);
            model.addAttribute("loginEnabled", user.isLoginEnabled());
            return "user-form";
        }

        form.applyTo(user);
        applyPassword(form, user, !isSelf(me, id));
        // Administrators log in with a password, so a personal link is no longer needed
        if (user.isAdmin()) {
            user.clearAccessLink();
        }
        if (!saveUnlessEmailIsTaken(user, bindingResult)) {
            form.setPassword(null);
            model.addAttribute("userId", id);
            model.addAttribute("loginEnabled", user.isLoginEnabled());
            return "user-form";
        }

        redirectAttributes.addFlashAttribute("message", "ユーザーを更新しました。");
        return "redirect:/users/" + id;
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @AuthenticationPrincipal AppUserDetails me,
                             RedirectAttributes redirectAttributes) {
        User user = findUser(id);
        if (isSelf(me, id)) {
            redirectAttributes.addFlashAttribute("error", "自分自身は削除できません。");
            return "redirect:/users";
        }
        userRepository.delete(user);

        redirectAttributes.addFlashAttribute("message", "ユーザーを削除しました。");
        return "redirect:/users";
    }

    // --- user detail and assignments ---

    @GetMapping("/users/{id}")
    public String showUser(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me, Model model) {
        User user = findUser(id);
        model.addAttribute("self", isSelf(me, id));
        List<TaskAssignment> assignments = assignmentService.findAssignments(id);

        model.addAttribute("user", user);
        model.addAttribute("assignments", assignments);
        model.addAttribute("checklists", checklistService.checklistsOf(assignments));
        model.addAttribute("progress", assignmentService.summarize(assignments));
        model.addAttribute("assignableTasks", assignmentService.findAssignableTasks(id));
        model.addAttribute("statuses", AssignmentStatus.values());
        return "user-detail";
    }

    // --- password reset ---

    /**
     * Sets a new random temporary password, shown only once on the next page.
     * The user must change it after logging in. For users who log in with a password.
     */
    @PostMapping("/users/{id}/password-reset")
    public String resetPassword(@PathVariable Long id,
                                @AuthenticationPrincipal AppUserDetails me,
                                RedirectAttributes redirectAttributes) {
        User user = findUser(id);
        if (!user.isAdmin() && loginSettings.isLinkMode()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "New employees log in with personal links");
        }
        if (isSelf(me, id)) {
            redirectAttributes.addFlashAttribute("error", "自分のパスワードは「パスワード変更」画面で変更してください。");
            return "redirect:/users/" + id;
        }
        String temporaryPassword = temporaryPasswords.generate();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setPasswordChangeRequired(true);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("temporaryPassword", temporaryPassword);
        redirectAttributes.addFlashAttribute("message", "仮パスワードを発行しました。");
        return "redirect:/users/" + id;
    }

    // --- personal link (when new employees log in with links) ---

    /** Issues a new personal link. The link is shown only once, on the next page. */
    @PostMapping("/users/{id}/access-link")
    public String issueAccessLink(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = findUserForAccessLink(id);
        if (user.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "管理者には専用URLを発行できません(管理者はパスワードでログインします)。");
            return "redirect:/users/" + id;
        }
        String token = accessLinkService.issue(user);

        redirectAttributes.addFlashAttribute("accessLink", appBaseUrl() + "/join/" + token);
        redirectAttributes.addFlashAttribute("message", "専用URLを発行しました。");
        return "redirect:/users/" + id;
    }

    @PostMapping("/users/{id}/access-link/delete")
    public String revokeAccessLink(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = findUserForAccessLink(id);
        accessLinkService.revoke(user);

        redirectAttributes.addFlashAttribute("message", "専用URLを無効にしました。");
        return "redirect:/users/" + id;
    }

    private User findUserForAccessLink(Long id) {
        if (!loginSettings.isLinkMode()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Personal links are not used");
        }
        return findUser(id);
    }

    /** e.g. https://example.com, without a trailing slash */
    private String appBaseUrl() {
        String url = baseUrl.isBlank()
                ? ServletUriComponentsBuilder.fromCurrentContextPath().toUriString()
                : baseUrl.strip();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
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
        try {
            int count = assignmentService.assignAllActive(user, dueDate);
            if (count == 0) {
                redirectAttributes.addFlashAttribute("error", "割り当てられる有効なタスクはありません。");
            } else {
                redirectAttributes.addFlashAttribute("message", count + "件のタスクを割り当てました。");
            }
        } catch (AssignmentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
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

    private void checkPasswordLength(UserForm form, BindingResult bindingResult) {
        if (form.hasNewPassword() && form.getPassword().length() < UserForm.MIN_PASSWORD_LENGTH
                && !bindingResult.hasFieldErrors("password")) {
            bindingResult.rejectValue("password", "short",
                    "パスワードは" + UserForm.MIN_PASSWORD_LENGTH + "文字以上で入力してください");
        }
    }

    /**
     * Saves a new password if one was entered. A password set for someone else must be changed
     * by that person at the next login (the administrator knows it).
     */
    private void applyPassword(UserForm form, User user, boolean forSomeoneElse) {
        if (form.hasNewPassword()) {
            user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
            user.setPasswordChangeRequired(forSomeoneElse);
        }
    }

    private boolean isSelf(AppUserDetails me, Long userId) {
        return me != null && userId.equals(me.getUserId());
    }

    /**
     * Saves the user, or reports a duplicate e-mail address when another request registered the same
     * address after the check above (two administrators at the same time): the database decides.
     */
    private boolean saveUnlessEmailIsTaken(User user, BindingResult bindingResult) {
        try {
            userRepository.saveAndFlush(user);
            return true;
        } catch (DataIntegrityViolationException e) {
            if (!String.valueOf(e.getMostSpecificCause().getMessage()).contains("email")) {
                throw e;
            }
            rejectDuplicateEmail(bindingResult);
            return false;
        }
    }

    private void rejectDuplicateEmail(BindingResult bindingResult) {
        bindingResult.rejectValue("email", "duplicate", "このメールアドレスは既に登録されています");
    }
}
