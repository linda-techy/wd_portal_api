package com.wd.api.model.scheduling;

import com.wd.api.model.BaseEntity;
import com.wd.api.model.enums.DependencyType;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@SQLDelete(sql = "UPDATE task_predecessor SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
// Uniqueness of (successor_id, predecessor_id) is enforced by the partial
// unique index `uq_task_predecessor_pair_live` (see V154), not a table-level
// @UniqueConstraint here. JPA doesn't express partial unique indexes —
// declaring it at entity level re-introduced V112's bug where soft-deleted
// rows still occupied the constraint and blocked re-inserts.
@Table(name = "task_predecessor")
public class TaskPredecessor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "successor_id", nullable = false)
    private Long successorId;

    @Column(name = "predecessor_id", nullable = false)
    private Long predecessorId;

    @Column(name = "lag_days", nullable = false)
    private Integer lagDays = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "dep_type", nullable = false, length = 2)
    private DependencyType depType = DependencyType.FS;

    public TaskPredecessor() {}

    public TaskPredecessor(Long successorId, Long predecessorId, Integer lagDays) {
        this.successorId = successorId;
        this.predecessorId = predecessorId;
        this.lagDays = lagDays == null ? 0 : lagDays;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSuccessorId() { return successorId; }
    public void setSuccessorId(Long successorId) { this.successorId = successorId; }
    public Long getPredecessorId() { return predecessorId; }
    public void setPredecessorId(Long predecessorId) { this.predecessorId = predecessorId; }
    public Integer getLagDays() { return lagDays; }
    public void setLagDays(Integer lagDays) { this.lagDays = lagDays == null ? 0 : lagDays; }
    public DependencyType getDepType() { return depType; }
    public void setDepType(DependencyType depType) { this.depType = depType == null ? DependencyType.FS : depType; }
}
