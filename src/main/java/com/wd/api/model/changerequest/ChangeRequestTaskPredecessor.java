package com.wd.api.model.changerequest;

import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Edge in a Change Request's proposed-task dependency graph. Cloned into
 * task_predecessor on merge.
 */
@SQLDelete(sql = "UPDATE change_request_task_predecessors SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
@Table(name = "change_request_task_predecessors",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_crtp_pair",
               columnNames = {"successor_cr_task_id", "predecessor_cr_task_id"}))
public class ChangeRequestTaskPredecessor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "successor_cr_task_id", nullable = false)
    private Long successorCrTaskId;

    @Column(name = "predecessor_cr_task_id", nullable = false)
    private Long predecessorCrTaskId;

    @Column(name = "lag_days", nullable = false)
    private Integer lagDays = 0;

    @Column(name = "dep_type", nullable = false, length = 2)
    private String depType = "FS";

    public ChangeRequestTaskPredecessor() {}

    public ChangeRequestTaskPredecessor(Long succId, Long predId, Integer lagDays) {
        this.successorCrTaskId = succId;
        this.predecessorCrTaskId = predId;
        this.lagDays = lagDays == null ? 0 : lagDays;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSuccessorCrTaskId() { return successorCrTaskId; }
    public void setSuccessorCrTaskId(Long id) { this.successorCrTaskId = id; }
    public Long getPredecessorCrTaskId() { return predecessorCrTaskId; }
    public void setPredecessorCrTaskId(Long id) { this.predecessorCrTaskId = id; }
    public Integer getLagDays() { return lagDays; }
    public void setLagDays(Integer l) { this.lagDays = l == null ? 0 : l; }
    public String getDepType() { return depType; }
    public void setDepType(String d) { this.depType = d == null ? "FS" : d; }
}
