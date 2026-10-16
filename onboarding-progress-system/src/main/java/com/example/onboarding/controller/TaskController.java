package com.example.onboarding.controller;

import com.example.onboarding.entity.Task;
import com.example.onboarding.repository.TaskRepository;
import com.example.onboarding.service.TaskOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final TaskOrderService taskOrderService;

    public TaskController(TaskRepository taskRepository, TaskOrderService taskOrderService) {
        this.taskRepository = taskRepository;
        this.taskOrderService = taskOrderService;
    }

    @GetMapping
    public List<Task> getAllTasks() {
        return taskRepository.findAllInDisplayOrder();
    }

    @GetMapping("/{id}")
    public Task getTask(@PathVariable Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    /**
     * JSON body of POST and PUT. Only these fields can be set from outside, so an id or timestamps
     * in the body are ignored, and a left-out "active" can be told apart from "active": true.
     */
    record TaskRequest(
            @NotBlank(message = "title is required")
            @Size(max = 200, message = "title must be at most 200 characters")
            String title,
            String description,
            Boolean active,
            Integer sortOrder) {
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setActive(request.active() != null ? request.active() : true);
        task.setSortOrder(request.sortOrder() != null ? request.sortOrder() : taskOrderService.nextSortOrder());
        Task saved = taskRepository.save(task);
        // 201 Created with the address of the new task (path only, like the redirects of the screens)
        return ResponseEntity.created(URI.create("/api/tasks/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        task.setTitle(request.title());
        task.setDescription(request.description());
        // Left out: keep the current value (a disabled task must not become active by accident)
        if (request.active() != null) {
            task.setActive(request.active());
        }
        if (request.sortOrder() != null) {
            task.setSortOrder(request.sortOrder());
        }

        return taskRepository.save(task);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        taskRepository.deleteById(id);
    }
}
