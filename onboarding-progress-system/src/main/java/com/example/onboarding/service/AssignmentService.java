package com.example.onboarding.service;

import com.example.onboarding.entity.AssignmentStatus;
import com.example.onboarding.entity.Task;
import com.example.onboarding.entity.TaskAssignment;
import com.example.onboarding.entity.User;
import com.example.onboarding.repository.TaskAssignmentRepository;
import com.example.onboarding.repository.TaskRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rules for assigning tasks to users and tracking their progress.
 */
@Service
@Transactional
public class AssignmentService {

    private final TaskAssignmentRepository assignmentRepository;
    private final TaskRepository taskRepository;
    private final Clock clock;

    public AssignmentService(TaskAssignmentRepository assignmentRepository,
                             TaskRepository taskRepository,
                             Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TaskAssignment> findAssignments(Long userId) {
        return assignmentRepository.findByUserIdOrderByTaskIdAsc(userId);
    }

    @Transactional(readOnly = true)
    public Optional<TaskAssignment> findAssignment(Long userId, Long assignmentId) {
        return assignmentRepository.findByIdAndUserId(assignmentId, userId);
    }

    /** Active tasks that are not assigned to the user yet, ordered by id. */
    @Transactional(readOnly = true)
    public List<Task> findAssignableTasks(Long userId) {
        Set<Long> assignedTaskIds = findAssignments(userId).stream()
                .map(assignment -> assignment.getTask().getId())
                .collect(Collectors.toSet());
        return taskRepository.findAll(Sort.by("id")).stream()
                .filter(task -> Boolean.TRUE.equals(task.getActive()))
                .filter(task -> !assignedTaskIds.contains(task.getId()))
                .toList();
    }

    /** Progress of every user that has at least one assignment, keyed by user id. */
    @Transactional(readOnly = true)
    public Map<Long, UserProgress> progressByUser() {
        return assignmentRepository.countProgressByUser(AssignmentStatus.DONE).stream()
                .collect(Collectors.toMap(
                        TaskAssignmentRepository.ProgressRow::getUserId,
                        row -> new UserProgress(row.getTotal(), row.getDone())));
    }

    public TaskAssignment assign(User user, Long taskId, LocalDate dueDate) {
        if (taskId == null) {
            throw new AssignmentException("割り当てるタスクを選択してください。");
        }
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AssignmentException("選択したタスクが見つかりません。"));
        if (!Boolean.TRUE.equals(task.getActive())) {
            throw new AssignmentException("無効(Active: No)のタスクは割り当てられません。");
        }
        if (assignmentRepository.existsByUserIdAndTaskId(user.getId(), taskId)) {
            throw new AssignmentException("このタスクは既に割り当てられています。");
        }
        return assignmentRepository.save(new TaskAssignment(user, task, dueDate));
    }

    /** Assigns every active task the user does not have yet. Returns how many were added. */
    public int assignAllActive(User user, LocalDate dueDate) {
        List<Task> tasks = findAssignableTasks(user.getId());
        tasks.forEach(task -> assignmentRepository.save(new TaskAssignment(user, task, dueDate)));
        return tasks.size();
    }

    public void changeStatus(TaskAssignment assignment, AssignmentStatus status) {
        assignment.changeStatus(status, Instant.now(clock));
        assignmentRepository.save(assignment);
    }

    public void unassign(TaskAssignment assignment) {
        assignmentRepository.delete(assignment);
    }
}
