package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Execution checkpoint milestone within an approved Change Order.
 * Only relevant once CO status = IN_PROGRESS.
 */
@Entity
@Table(name = "change_order_milestones")
public class ChangeOrderMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_order_id", nullable = false)
    private ChangeOrder changeOrder;

    @Column(name = "milestone_number", nullable = false)
    private Integer milestoneNumber;

    @Column(nullable = false, length = 255)
    private String description;

    /** Percentage of CO value due at this milestone (e.g. 0.3000 = 30%). */
    @Column(precision = 6, scale = 4, nullable = false)
    private BigDecimal percentage;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private PortalUser completedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ChangeOrder getChangeOrder() { return changeOrder; }
    public void setChangeOrder(ChangeOrder changeOrder) { this.changeOrder = changeOrder; }

    public Integer getMilestoneNumber() { return milestoneNumber; }
    public void setMilestoneNumber(Integer milestoneNumber) { this.milestoneNumber = milestoneNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public PortalUser getCompletedBy() { return completedBy; }
    public void setCompletedBy(PortalUser completedBy) { this.completedBy = completedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
