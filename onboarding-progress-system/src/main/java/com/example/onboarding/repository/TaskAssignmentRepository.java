package com.example.onboarding.repository;

import com.example.onboarding.entity.AssignmentStatus;
import com.example.onboarding.entity.TaskAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    /** Assignments of one user, with their tasks loaded, ordered by task id. */
    @EntityGraph(attributePaths = "task")
    List<TaskAssignment> findByUserIdOrderByTaskIdAsc(Long userId);

    /** Assignments of one user, with their tasks loaded, in the task display order. */
    @EntityGraph(attributePaths = "task")
    List<TaskAssignment> findByUserIdOrderByTaskSortOrderAscTaskIdAsc(Long userId);

    @EntityGraph(attributePaths = "task")
    Optional<TaskAssignment> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndTaskId(Long userId, Long taskId);

    /** Number of assigned, completed and overdue tasks per user. */
    @Query("""
            select a.user.id as userId,
                   count(a) as total,
                   sum(case when a.status = :done then 1 else 0 end) as done,
                   sum(case when a.status <> :done and a.dueDate < :today then 1 else 0 end) as overdue
            from TaskAssignment a
            group by a.user.id
            """)
    List<ProgressRow> countProgressByUser(@Param("done") AssignmentStatus done, @Param("today") LocalDate today);

    interface ProgressRow {
        Long getUserId();

        long getTotal();

        long getDone();

        long getOverdue();
    }
}
