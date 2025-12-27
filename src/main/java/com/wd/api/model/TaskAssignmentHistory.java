package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks the complete history of task assignments.
 * Every time a task is assigned/reassigned/unassigned, a record is created.
 * This provides full audit trail for transparency and accountability.
 */
@Entity
@Table(name = "task_assignment_history")
public class TaskAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_from_id")
    private User assignedFrom; // Previous assignee (null if task was unassigned)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo; // New assignee (null if being unassigned)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy; // Who made this assignment change

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // Optional explanation for assignment change

    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }

    // Constructors
    public TaskAssignmentHistory() {
    }

    public TaskAssignmentHistory(Long taskId, User assignedFrom, User assignedTo, User assignedBy, String notes) {
        this.taskId = taskId;
        this.assignedFrom = assignedFrom;
        this.assignedTo = assignedTo;
        this.assignedBy = assignedBy;
        this.notes = notes;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public User getAssignedFrom() {
        return assignedFrom;
    }

    public void setAssignedFrom(User assignedFrom) {
        this.assignedFrom = assignedFrom;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }

    public User getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
