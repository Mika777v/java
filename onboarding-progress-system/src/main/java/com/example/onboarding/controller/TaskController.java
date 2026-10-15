package com.example.onboarding.controller;

import com.example.onboarding.entity.Task;
import com.example.onboarding.repository.TaskRepository;
import com.example.onboarding.service.TaskOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task createTask(@Valid @RequestBody Task task) {
        // Always create a new task: an id in the body must not overwrite an existing task
        task.setId(null);
        if (task.getActive() == null) {
            task.setActive(true);
        }
        if (task.getSortOrder() == null) {
            task.setSortOrder(taskOrderService.nextSortOrder());
        }
        return taskRepository.save(task);
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable Long id, @Valid @RequestBody Task request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setActive(request.getActive() != null ? request.getActive() : true);
        if (request.getSortOrder() != null) {
            task.setSortOrder(request.getSortOrder());
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
