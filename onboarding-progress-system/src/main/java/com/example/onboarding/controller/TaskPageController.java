package com.example.onboarding.controller;

import com.example.onboarding.entity.Task;
import com.example.onboarding.form.TaskForm;
import com.example.onboarding.repository.TaskRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TaskPageController {

    private final TaskRepository taskRepository;

    public TaskPageController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/tasks";
    }

    @GetMapping("/tasks")
    public String listTasks(Model model) {
        model.addAttribute("tasks", taskRepository.findAll(Sort.by("id")));
        return "tasks";
    }

    @GetMapping("/tasks/new")
    public String newTask(Model model) {
        model.addAttribute("taskForm", new TaskForm());
        return "task-form";
    }

    @PostMapping("/tasks")
    public String createTask(@Valid @ModelAttribute("taskForm") TaskForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "task-form";
        }

        Task task = new Task();
        form.applyTo(task);
        taskRepository.save(task);

        redirectAttributes.addFlashAttribute("message", "タスクを登録しました。");
        return "redirect:/tasks";
    }

    @GetMapping("/tasks/{id}/edit")
    public String editTask(@PathVariable Long id, Model model) {
        Task task = findTask(id);
        model.addAttribute("taskId", id);
        model.addAttribute("taskForm", TaskForm.from(task));
        return "task-form";
    }

    @PostMapping("/tasks/{id}")
    public String updateTask(@PathVariable Long id,
                             @Valid @ModelAttribute("taskForm") TaskForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        Task task = findTask(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("taskId", id);
            return "task-form";
        }

        form.applyTo(task);
        taskRepository.save(task);

        redirectAttributes.addFlashAttribute("message", "タスクを更新しました。");
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Task task = findTask(id);
        taskRepository.delete(task);

        redirectAttributes.addFlashAttribute("message", "タスクを削除しました。");
        return "redirect:/tasks";
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }
}
