package com.example.onboarding.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A sub-item of a task (e.g. "Teams" in "Install apps on the phone").
 * The items belong to the task, so every user the task is assigned to sees the same items.
 */
@Entity
@Table(name = "task_checklist_items")
public class TaskChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskChecklistItem() {
    }

    public TaskChecklistItem(Task task, String label, int sortOrder) {
        this.task = task;
        this.label = label;
        this.sortOrder = sortOrder;
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

    public Long getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public String getLabel() {
        return label;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}
