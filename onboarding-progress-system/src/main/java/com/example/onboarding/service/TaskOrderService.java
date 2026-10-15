package com.example.onboarding.service;

import com.example.onboarding.entity.Task;
import com.example.onboarding.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Display order of tasks. Orders are kept as 10, 20, 30, ... so that they stay easy to read.
 */
@Service
@Transactional
public class TaskOrderService {

    private static final int STEP = 10;

    private final TaskRepository taskRepository;

    public TaskOrderService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /** Order value that puts a new task at the end. */
    @Transactional(readOnly = true)
    public int nextSortOrder() {
        return taskRepository.findMaxSortOrder() + STEP;
    }

    /**
     * Moves the task one position up (offset -1) or down (offset +1).
     * Returns false when it is already at the top / bottom.
     */
    public boolean move(Task task, int offset) {
        List<Task> tasks = new ArrayList<>(taskRepository.findAllInDisplayOrder());
        int index = -1;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(task.getId())) {
                index = i;
                break;
            }
        }
        int target = index + offset;
        if (index < 0 || target < 0 || target >= tasks.size()) {
            return false;
        }

        Collections.swap(tasks, index, target);
        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).setSortOrder((i + 1) * STEP);
        }
        taskRepository.saveAll(tasks);
        return true;
    }
}
