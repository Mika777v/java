package com.example.onboarding.repository;

import com.example.onboarding.entity.TaskChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskChecklistItemRepository extends JpaRepository<TaskChecklistItem, Long> {

    List<TaskChecklistItem> findByTaskIdOrderBySortOrderAscIdAsc(Long taskId);

    /** Items of several tasks at once (one query for a whole list of assignments). */
    List<TaskChecklistItem> findByTaskIdInOrderBySortOrderAscIdAsc(Collection<Long> taskIds);

    Optional<TaskChecklistItem> findByIdAndTaskId(Long id, Long taskId);

    @Query("SELECT COALESCE(MAX(i.sortOrder), 0) FROM TaskChecklistItem i WHERE i.task.id = :taskId")
    int findMaxSortOrder(Long taskId);
}
