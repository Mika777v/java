package com.example.onboarding.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A checked sub-item of an assignment. The row exists only while the item is checked.
 */
@Entity
@Table(name = "assignment_checks")
public class AssignmentCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private TaskAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private TaskChecklistItem item;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    protected AssignmentCheck() {
    }

    public AssignmentCheck(TaskAssignment assignment, TaskChecklistItem item, Instant checkedAt) {
        this.assignment = assignment;
        this.item = item;
        this.checkedAt = checkedAt;
    }

    public Long getId() {
        return id;
    }

    public TaskAssignment getAssignment() {
        return assignment;
    }

    public TaskChecklistItem getItem() {
        return item;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }
}
