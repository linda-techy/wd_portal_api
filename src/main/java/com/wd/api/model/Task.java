package com.wd.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority = TaskPriority.MEDIUM;

    /**
     * Lazy-loaded relationships - excluded from JSON serialization to prevent lazy-loading proxy issues
     * These are managed via foreign key columns in the database
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private PortalUser assignedTo;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private PortalUser createdBy;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private CustomerProject project;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    /**
     * MANDATORY: Task completion deadline
     * 
     * Business Rationale (Construction Domain):
     * - Every construction task requires a deadline for project timeline tracking
     * - Enables proactive alert system for overdue/approaching deadlines
     * - Critical for resource planning and performance accountability
     * - Supports manager dashboards and project timeline views
     * 
     * Validation: Must be present and >= task creation date
     */
    @jakarta.validation.constraints.NotNull(message = "Due date is mandatory for task accountability and project timeline tracking")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    // Constructors
    public Task() {
    }

    public Task(Long id, String title, String description, TaskStatus status, TaskPriority priority,
            PortalUser assignedTo, PortalUser createdBy, CustomerProject project, Lead lead,
            LocalDate dueDate) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.assignedTo = assignedTo;
        this.createdBy = createdBy;
        this.project = project;
        this.lead = lead;
        this.dueDate = dueDate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public PortalUser getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(PortalUser assignedTo) {
        this.assignedTo = assignedTo;
    }

    public PortalUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(PortalUser createdBy) {
        this.createdBy = createdBy;
    }

    public CustomerProject getProject() {
        return project;
    }

    public void setProject(CustomerProject project) {
        this.project = project;
    }

    public Lead getLead() {
        return lead;
    }

    public void setLead(Lead lead) {
        this.lead = lead;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    // Enums
    public enum TaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    public enum TaskPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
}
