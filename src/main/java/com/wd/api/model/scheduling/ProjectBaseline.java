package com.wd.api.model.scheduling;

import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;

@SQLDelete(sql = "UPDATE project_baseline SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
@Table(name = "project_baseline",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_project_baseline_project_id",
               columnNames = "project_id"))
public class ProjectBaseline extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "approved_at", nullable = false)
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", nullable = false)
    private Long approvedBy;

    @Column(name = "project_start_date", nullable = false)
    private LocalDate projectStartDate;

    @Column(name = "project_finish_date", nullable = false)
    private LocalDate projectFinishDate;

    public ProjectBaseline() {}

    public ProjectBaseline(Long projectId, LocalDateTime approvedAt, Long approvedBy,
                           LocalDate projectStartDate, LocalDate projectFinishDate) {
        this.projectId = projectId;
        this.approvedAt = approvedAt;
        this.approvedBy = approvedBy;
        this.projectStartDate = projectStartDate;
        this.projectFinishDate = projectFinishDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public LocalDate getProjectStartDate() { return projectStartDate; }
    public void setProjectStartDate(LocalDate projectStartDate) { this.projectStartDate = projectStartDate; }
    public LocalDate getProjectFinishDate() { return projectFinishDate; }
    public void setProjectFinishDate(LocalDate projectFinishDate) { this.projectFinishDate = projectFinishDate; }
}
