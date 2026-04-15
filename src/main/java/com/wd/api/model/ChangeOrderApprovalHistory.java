package com.wd.api.model;

import com.wd.api.model.enums.ApprovalAction;
import com.wd.api.model.enums.ApprovalLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable audit trail for multi-level VO approval.
 *
 * Rules:
 *  - INSERT only. No UPDATE or DELETE ever.
 *  - One row per action (APPROVED, REJECTED, ESCALATED, RETURNED).
 *  - Level drives threshold: PM < ₹7.5L, COMMERCIAL_MANAGER ₹7.5L–37.5L, DIRECTOR > ₹37.5L
 */
@Entity
@Table(name = "co_approval_history")
public class ChangeOrderApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "co_id", nullable = false)
    private ChangeOrder changeOrder;

    @Column(name = "approver_name", nullable = false, length = 100)
    private String approverName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private PortalUser approver;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 30)
    private ApprovalLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private ApprovalAction action;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "action_at", nullable = false, updatable = false)
    private LocalDateTime actionAt;

    @PrePersist
    protected void onCreate() {
        if (actionAt == null) actionAt = LocalDateTime.now();
    }

    // ---- Getters (no setters for immutable fields; setters provided for builder pattern) ----

    public Long getId() { return id; }

    public ChangeOrder getChangeOrder() { return changeOrder; }
    public void setChangeOrder(ChangeOrder changeOrder) { this.changeOrder = changeOrder; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public PortalUser getApprover() { return approver; }
    public void setApprover(PortalUser approver) { this.approver = approver; }

    public ApprovalLevel getLevel() { return level; }
    public void setLevel(ApprovalLevel level) { this.level = level; }

    public ApprovalAction getAction() { return action; }
    public void setAction(ApprovalAction action) { this.action = action; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getActionAt() { return actionAt; }
}
