package com.wd.api.model.scheduling;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Edge in a WBS template's task dependency graph. Successor cannot start until
 * predecessor finishes (FS by default, with optional lag in days). Cloned into
 * task_predecessor at project-creation time with cross-floor expansion rules
 * applied (see WbsTemplateClonerService).
 */
@Entity
@Table(name = "wbs_template_task_predecessor")
public class WbsTemplateTaskPredecessor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "successor_template_task_id", nullable = false)
    private WbsTemplateTask successor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predecessor_template_task_id", nullable = false)
    private WbsTemplateTask predecessor;

    @Column(name = "lag_days", nullable = false)
    private Integer lagDays = 0;

    @Column(name = "dep_type", nullable = false, length = 2)
    private String depType = "FS";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (lagDays == null) lagDays = 0;
        if (depType == null) depType = "FS";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public WbsTemplateTask getSuccessor() { return successor; }
    public void setSuccessor(WbsTemplateTask successor) { this.successor = successor; }
    public WbsTemplateTask getPredecessor() { return predecessor; }
    public void setPredecessor(WbsTemplateTask predecessor) { this.predecessor = predecessor; }
    public Integer getLagDays() { return lagDays; }
    public void setLagDays(Integer lagDays) { this.lagDays = lagDays == null ? 0 : lagDays; }
    public String getDepType() { return depType; }
    public void setDepType(String depType) { this.depType = depType == null ? "FS" : depType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
