package com.wd.api.model.changerequest;

import com.wd.api.model.BaseEntity;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.enums.FloorLoop;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;

/**
 * Proposed task on a Change Request (ProjectVariation). Author-time scope only;
 * cloned into `tasks` by ChangeRequestMergeService on the
 * APPROVED -> SCHEDULED transition. Mirrors the shape of WbsTemplateTask.
 */
@SQLDelete(sql = "UPDATE change_request_tasks SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
@Table(name = "change_request_tasks")
@Check(constraints = "duration_days >= 1 AND floor_loop IN ('NONE','PER_FLOOR')")
public class ChangeRequestTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ProjectVariation changeRequest;

    @Column(nullable = false)
    private Integer sequence;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(name = "role_hint", length = 64)
    private String roleHint;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "weight_factor")
    private Integer weightFactor;

    @Column(name = "monsoon_sensitive", nullable = false)
    private Boolean monsoonSensitive = Boolean.FALSE;

    @Column(name = "is_payment_milestone", nullable = false)
    private Boolean isPaymentMilestone = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "floor_loop", nullable = false, length = 16)
    private FloorLoop floorLoop = FloorLoop.NONE;

    @Column(name = "optional_cost", precision = 14, scale = 2)
    private BigDecimal optionalCost;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProjectVariation getChangeRequest() { return changeRequest; }
    public void setChangeRequest(ProjectVariation cr) { this.changeRequest = cr; }
    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRoleHint() { return roleHint; }
    public void setRoleHint(String roleHint) { this.roleHint = roleHint; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer days) { this.durationDays = days; }
    public Integer getWeightFactor() { return weightFactor; }
    public void setWeightFactor(Integer w) { this.weightFactor = w; }
    public Boolean getMonsoonSensitive() { return monsoonSensitive; }
    public void setMonsoonSensitive(Boolean m) {
        this.monsoonSensitive = m == null ? Boolean.FALSE : m;
    }
    public Boolean getIsPaymentMilestone() { return isPaymentMilestone; }
    public void setIsPaymentMilestone(Boolean v) {
        this.isPaymentMilestone = v == null ? Boolean.FALSE : v;
    }
    public FloorLoop getFloorLoop() { return floorLoop; }
    public void setFloorLoop(FloorLoop f) {
        this.floorLoop = f == null ? FloorLoop.NONE : f;
    }
    public BigDecimal getOptionalCost() { return optionalCost; }
    public void setOptionalCost(BigDecimal c) { this.optionalCost = c; }
}
