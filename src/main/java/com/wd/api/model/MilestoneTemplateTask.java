package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "milestone_template_tasks",
       uniqueConstraints = @UniqueConstraint(columnNames = {"milestone_template_id", "task_order"}))
public class MilestoneTemplateTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_template_id", nullable = false)
    private MilestoneTemplate milestoneTemplate;

    @Column(name = "task_name", nullable = false, length = 255)
    private String taskName;

    @Column(name = "task_order", nullable = false)
    private Integer taskOrder;

    @Column(name = "estimated_days")
    private Integer estimatedDays;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public MilestoneTemplateTask() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public MilestoneTemplate getMilestoneTemplate() { return milestoneTemplate; }
    public String getTaskName() { return taskName; }
    public Integer getTaskOrder() { return taskOrder; }
    public Integer getEstimatedDays() { return estimatedDays; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
