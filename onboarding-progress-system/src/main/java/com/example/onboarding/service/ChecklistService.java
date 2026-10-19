package com.example.onboarding.service;

import com.example.onboarding.entity.AssignmentStatus;
import com.example.onboarding.entity.Task;
import com.example.onboarding.entity.TaskAssignment;
import com.example.onboarding.entity.TaskChecklistItem;
import com.example.onboarding.repository.AssignmentCheckRepository;
import com.example.onboarding.repository.TaskChecklistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sub-items of tasks (a checklist) and the checks of each assignment.
 * Checking items also moves the assignment forward: some checked -> 対応中, all checked -> 完了.
 */
@Service
@Transactional
public class ChecklistService {

    private static final int STEP = 10;

    private final TaskChecklistItemRepository itemRepository;
    private final AssignmentCheckRepository checkRepository;
    private final AssignmentService assignmentService;
    private final Clock clock;

    public ChecklistService(TaskChecklistItemRepository itemRepository,
                            AssignmentCheckRepository checkRepository,
                            AssignmentService assignmentService,
                            Clock clock) {
        this.itemRepository = itemRepository;
        this.checkRepository = checkRepository;
        this.assignmentService = assignmentService;
        this.clock = clock;
    }

    /** Result of checking or unchecking an item, for the message on the page. */
    public record CheckResult(TaskChecklistItem item, boolean checked, AssignmentStatus newStatus) {
    }

    @Transactional(readOnly = true)
    public List<TaskChecklistItem> findItems(Long taskId) {
        return itemRepository.findByTaskIdOrderBySortOrderAscIdAsc(taskId);
    }

    /** Adds an item at the end. Users the task is already assigned to see it too (unchecked). */
    public TaskChecklistItem addItem(Task task, String label) {
        int sortOrder = itemRepository.findMaxSortOrder(task.getId()) + STEP;
        return itemRepository.save(new TaskChecklistItem(task, label, sortOrder));
    }

    /** Deletes an item of the task; its checks are deleted by the database. Empty when not found. */
    public Optional<TaskChecklistItem> deleteItem(Long taskId, Long itemId) {
        Optional<TaskChecklistItem> item = itemRepository.findByIdAndTaskId(itemId, taskId);
        item.ifPresent(itemRepository::delete);
        return item;
    }

    /**
     * Checklists of the given assignments, keyed by assignment id.
     * Two queries in total, however many assignments there are.
     */
    @Transactional(readOnly = true)
    public Map<Long, Checklist> checklistsOf(List<TaskAssignment> assignments) {
        if (assignments.isEmpty()) {
            return Map.of();
        }
        Set<Long> taskIds = assignments.stream().map(a -> a.getTask().getId()).collect(Collectors.toSet());
        Map<Long, List<TaskChecklistItem>> itemsByTask = itemRepository.findByTaskIdInOrderBySortOrderAscIdAsc(taskIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getTask().getId()));
        Set<String> checked = new HashSet<>();
        for (Object[] row : checkRepository.findCheckedItemIds(assignments.stream().map(TaskAssignment::getId).toList())) {
            checked.add(row[0] + ":" + row[1]);
        }

        Map<Long, Checklist> result = new HashMap<>();
        for (TaskAssignment assignment : assignments) {
            List<Checklist.Item> items = itemsByTask.getOrDefault(assignment.getTask().getId(), List.of()).stream()
                    .map(item -> new Checklist.Item(item.getId(), item.getLabel(),
                            checked.contains(assignment.getId() + ":" + item.getId())))
                    .toList();
            result.put(assignment.getId(), new Checklist(items));
        }
        return result;
    }

    /**
     * Checks or unchecks the item and updates the status of the assignment.
     * Empty when the item does not belong to the task of the assignment.
     */
    public Optional<CheckResult> setChecked(TaskAssignment assignment, Long itemId, boolean checked) {
        Optional<TaskChecklistItem> found = itemRepository.findByIdAndTaskId(itemId, assignment.getTask().getId());
        if (found.isEmpty()) {
            return Optional.empty();
        }
        if (checked) {
            checkRepository.check(assignment.getId(), itemId, Instant.now(clock));
        } else {
            checkRepository.uncheck(assignment.getId(), itemId);
        }

        AssignmentStatus newStatus = statusAfterCheck(assignment.getStatus(),
                checkRepository.countByAssignmentId(assignment.getId()),
                itemRepository.findByTaskIdOrderBySortOrderAscIdAsc(assignment.getTask().getId()).size());
        if (newStatus != assignment.getStatus()) {
            assignmentService.changeStatus(assignment, newStatus);
        }
        return Optional.of(new CheckResult(found.get(), checked, newStatus));
    }

    /**
     * Status after the number of checked items changed: all checked -> 完了, some checked -> 対応中.
     * When the last check is taken off a completed assignment it goes back to 対応中 (it was started).
     */
    static AssignmentStatus statusAfterCheck(AssignmentStatus current, long checked, int total) {
        if (total > 0 && checked >= total) {
            return AssignmentStatus.DONE;
        }
        if (checked > 0 || current == AssignmentStatus.DONE) {
            return AssignmentStatus.IN_PROGRESS;
        }
        return current;
    }
}
