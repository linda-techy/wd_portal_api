package com.wd.api.model.scheduling;

import com.wd.api.model.enums.FloorLoop;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wbs_template_task")
public class WbsTemplateTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id", nullable = false)
    private WbsTemplatePhase phase;

    @Column(nullable = false)
    private Integer sequence;

    @Column(nullable = false, length = 128)
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (monsoonSensitive == null) monsoonSensitive = Boolean.FALSE;
        if (isPaymentMilestone == null) isPaymentMilestone = Boolean.FALSE;
        if (floorLoop == null) floorLoop = FloorLoop.NONE;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public WbsTemplatePhase getPhase() { return phase; }
    public void setPhase(WbsTemplatePhase phase) { this.phase = phase; }
    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRoleHint() { return roleHint; }
    public void setRoleHint(String roleHint) { this.roleHint = roleHint; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public Integer getWeightFactor() { return weightFactor; }
    public void setWeightFactor(Integer weightFactor) { this.weightFactor = weightFactor; }
    public Boolean getMonsoonSensitive() { return monsoonSensitive; }
    public void setMonsoonSensitive(Boolean monsoonSensitive) {
        this.monsoonSensitive = monsoonSensitive == null ? Boolean.FALSE : monsoonSensitive;
    }
    public Boolean getIsPaymentMilestone() { return isPaymentMilestone; }
    public void setIsPaymentMilestone(Boolean isPaymentMilestone) {
        this.isPaymentMilestone = isPaymentMilestone == null ? Boolean.FALSE : isPaymentMilestone;
    }
    public FloorLoop getFloorLoop() { return floorLoop; }
    public void setFloorLoop(FloorLoop floorLoop) {
        this.floorLoop = floorLoop == null ? FloorLoop.NONE : floorLoop;
    }
    public BigDecimal getOptionalCost() { return optionalCost; }
    public void setOptionalCost(BigDecimal optionalCost) { this.optionalCost = optionalCost; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
