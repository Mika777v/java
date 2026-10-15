package com.example.onboarding.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A task assigned to a user, with its progress.
 */
@Entity
@Table(name = "task_assignments")
public class TaskAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.NOT_STARTED;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskAssignment() {
    }

    public TaskAssignment(User user, Task task, LocalDate dueDate) {
        this.user = user;
        this.task = task;
        this.dueDate = dueDate;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Changes the progress. The completion time is recorded when the task becomes DONE
     * and cleared when it is moved back to another status.
     */
    public void changeStatus(AssignmentStatus newStatus, Instant now) {
        if (newStatus == AssignmentStatus.DONE && this.status != AssignmentStatus.DONE) {
            this.completedAt = now;
        } else if (newStatus != AssignmentStatus.DONE) {
            this.completedAt = null;
        }
        this.status = newStatus;
    }

    /** True when the due date has passed and the task is not done yet. */
    public boolean isOverdue(LocalDate today) {
        return dueDate != null && status != AssignmentStatus.DONE && dueDate.isBefore(today);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Task getTask() {
        return task;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
