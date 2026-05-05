package com.wd.api.model.scheduling;

import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@SQLDelete(sql = "UPDATE task_predecessor SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
@Table(name = "task_predecessor",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_task_predecessor_pair",
               columnNames = {"successor_id", "predecessor_id"}))
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

    @Column(name = "dep_type", nullable = false, length = 2)
    private String depType = "FS";

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
    public String getDepType() { return depType; }
    public void setDepType(String depType) { this.depType = depType; }
}
