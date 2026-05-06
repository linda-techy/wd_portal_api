package com.wd.api.model.scheduling;

import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@SQLDelete(sql = "UPDATE task_baseline SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
@Table(name = "task_baseline",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_task_baseline_pair",
               columnNames = {"baseline_id", "task_id"}))
public class TaskBaseline extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "baseline_id", nullable = false)
    private Long baselineId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "task_name_at_baseline", nullable = false, length = 256)
    private String taskNameAtBaseline;

    @Column(name = "baseline_start", nullable = false)
    private LocalDate baselineStart;

    @Column(name = "baseline_end", nullable = false)
    private LocalDate baselineEnd;

    @Column(name = "baseline_duration_days", nullable = false)
    private Integer baselineDurationDays;

    public TaskBaseline() {}

    public TaskBaseline(Long baselineId, Long taskId, String taskNameAtBaseline,
                        LocalDate baselineStart, LocalDate baselineEnd, Integer baselineDurationDays) {
        this.baselineId = baselineId;
        this.taskId = taskId;
        this.taskNameAtBaseline = taskNameAtBaseline;
        this.baselineStart = baselineStart;
        this.baselineEnd = baselineEnd;
        this.baselineDurationDays = baselineDurationDays;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBaselineId() { return baselineId; }
    public void setBaselineId(Long baselineId) { this.baselineId = baselineId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getTaskNameAtBaseline() { return taskNameAtBaseline; }
    public void setTaskNameAtBaseline(String taskNameAtBaseline) { this.taskNameAtBaseline = taskNameAtBaseline; }
    public LocalDate getBaselineStart() { return baselineStart; }
    public void setBaselineStart(LocalDate baselineStart) { this.baselineStart = baselineStart; }
    public LocalDate getBaselineEnd() { return baselineEnd; }
    public void setBaselineEnd(LocalDate baselineEnd) { this.baselineEnd = baselineEnd; }
    public Integer getBaselineDurationDays() { return baselineDurationDays; }
    public void setBaselineDurationDays(Integer baselineDurationDays) { this.baselineDurationDays = baselineDurationDays; }
}
