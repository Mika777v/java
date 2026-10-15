package com.example.onboarding.repository;

import com.example.onboarding.entity.Task;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /** Display order: sort_order first, then id for tasks with the same order. */
    Sort DISPLAY_ORDER = Sort.by("sortOrder", "id");

    default List<Task> findAllInDisplayOrder() {
        return findAll(DISPLAY_ORDER);
    }

    @Query("select coalesce(max(t.sortOrder), 0) from Task t")
    int findMaxSortOrder();
}
