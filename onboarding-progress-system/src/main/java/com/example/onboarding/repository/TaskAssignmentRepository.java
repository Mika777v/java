package com.example.onboarding.repository;

import com.example.onboarding.entity.AssignmentStatus;
import com.example.onboarding.entity.TaskAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    /** Assignments of one user, with their tasks loaded, ordered by task id. */
    @EntityGraph(attributePaths = "task")
    List<TaskAssignment> findByUserIdOrderByTaskIdAsc(Long userId);

    @EntityGraph(attributePaths = "task")
    Optional<TaskAssignment> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndTaskId(Long userId, Long taskId);

    /** Number of assigned and completed tasks per user. */
    @Query("""
            select a.user.id as userId,
                   count(a) as total,
                   sum(case when a.status = :done then 1 else 0 end) as done
            from TaskAssignment a
            group by a.user.id
            """)
    List<ProgressRow> countProgressByUser(@Param("done") AssignmentStatus done);

    interface ProgressRow {
        Long getUserId();

        long getTotal();

        long getDone();
    }
}
