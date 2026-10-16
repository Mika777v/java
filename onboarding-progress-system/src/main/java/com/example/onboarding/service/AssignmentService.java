package com.example.onboarding.service;

import com.example.onboarding.entity.AssignmentStatus;
import com.example.onboarding.entity.Task;
import com.example.onboarding.entity.TaskAssignment;
import com.example.onboarding.entity.User;
import com.example.onboarding.repository.TaskAssignmentRepository;
import com.example.onboarding.repository.TaskRepository;
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final String ALREADY_ASSIGNED = "このタスクは既に割り当てられています。";

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
        return assignmentRepository.findByUserIdOrderByTaskSortOrderAscTaskIdAsc(userId);
    }

    @Transactional(readOnly = true)
    public Optional<TaskAssignment> findAssignment(Long userId, Long assignmentId) {
        return assignmentRepository.findByIdAndUserId(assignmentId, userId);
    }

    /** Active tasks that are not assigned to the user yet, in the task display order. */
    @Transactional(readOnly = true)
    public List<Task> findAssignableTasks(Long userId) {
        Set<Long> assignedTaskIds = findAssignments(userId).stream()
                .map(assignment -> assignment.getTask().getId())
                .collect(Collectors.toSet());
        return taskRepository.findAllInDisplayOrder().stream()
                .filter(task -> Boolean.TRUE.equals(task.getActive()))
                .filter(task -> !assignedTaskIds.contains(task.getId()))
                .toList();
    }

    /** Progress of every user that has at least one assignment, keyed by user id. */
    @Transactional(readOnly = true)
    public Map<Long, UserProgress> progressByUser() {
        return assignmentRepository.countProgressByUser(AssignmentStatus.DONE, LocalDate.now(clock)).stream()
                .collect(Collectors.toMap(
                        TaskAssignmentRepository.ProgressRow::getUserId,
                        row -> new UserProgress(row.getTotal(), row.getDone(), row.getOverdue())));
    }

    /** Progress of the given assignments (e.g. those of one user). */
    public UserProgress summarize(List<TaskAssignment> assignments) {
        LocalDate today = LocalDate.now(clock);
        long done = assignments.stream().filter(a -> a.getStatus() == AssignmentStatus.DONE).count();
        long overdue = assignments.stream().filter(a -> a.isOverdue(today)).count();
        return new UserProgress(assignments.size(), done, overdue);
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
            throw new AssignmentException(ALREADY_ASSIGNED);
        }
        return saveNew(new TaskAssignment(user, task, dueDate));
    }

    /** Assigns every active task the user does not have yet. Returns how many were added. */
    public int assignAllActive(User user, LocalDate dueDate) {
        List<Task> tasks = findAssignableTasks(user.getId());
        tasks.forEach(task -> saveNew(new TaskAssignment(user, task, dueDate)));
        return tasks.size();
    }

    /**
     * Two requests at the same time (a double click, or two administrators) can both pass the
     * "already assigned?" check. The unique constraint (user_id, task_id) then rejects the second one,
     * which is shown as the usual message instead of an error page.
     */
    private TaskAssignment saveNew(TaskAssignment assignment) {
        try {
            return assignmentRepository.saveAndFlush(assignment);
        } catch (DataIntegrityViolationException e) {
            throw new AssignmentException(ALREADY_ASSIGNED);
        }
    }

    public void changeStatus(TaskAssignment assignment, AssignmentStatus status) {
        assignment.changeStatus(status, Instant.now(clock));
        assignmentRepository.save(assignment);
    }

    public void unassign(TaskAssignment assignment) {
        assignmentRepository.delete(assignment);
    }
}
