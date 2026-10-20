package com.example.onboarding.repository;

import com.example.onboarding.entity.AssignmentCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface AssignmentCheckRepository extends JpaRepository<AssignmentCheck, Long> {

    /**
     * Checks an item. Doing it twice (a double click, or two pages open) does nothing the second time,
     * instead of failing on the unique constraint.
     */
    @Modifying
    @Query(value = "INSERT INTO assignment_checks (assignment_id, item_id, checked_at) VALUES (:assignmentId, :itemId, :checkedAt) "
            + "ON CONFLICT (assignment_id, item_id) DO NOTHING", nativeQuery = true)
    int check(Long assignmentId, Long itemId, Instant checkedAt);

    @Modifying
    @Query("DELETE FROM AssignmentCheck c WHERE c.assignment.id = :assignmentId AND c.item.id = :itemId")
    int uncheck(Long assignmentId, Long itemId);

    /** Checked items of several assignments at once: rows of [assignment id, item id]. */
    @Query("SELECT c.assignment.id, c.item.id FROM AssignmentCheck c WHERE c.assignment.id IN :assignmentIds")
    List<Object[]> findCheckedItemIds(Collection<Long> assignmentIds);

    long countByAssignmentId(Long assignmentId);
}
