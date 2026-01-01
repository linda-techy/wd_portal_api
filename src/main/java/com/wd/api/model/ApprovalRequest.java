package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_requests")
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_type", nullable = false)
    private String targetType; // PO, INVOICE, MB, CHALLAN

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private PortalUser requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private PortalUser approver;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
        if (status == null)
            status = "PENDING";
    }

    public ApprovalRequest() {
    }

    public ApprovalRequest(Long id, String targetType, Long targetId, PortalUser requestedBy, PortalUser approver,
            String status, String comments, LocalDateTime requestedAt, LocalDateTime decidedAt) {
        this.id = id;
        this.targetType = targetType;
        this.targetId = targetId;
        this.requestedBy = requestedBy;
        this.approver = approver;
        this.status = status;
        this.comments = comments;
        this.requestedAt = requestedAt;
        this.decidedAt = decidedAt;
    }

    public static ApprovalRequestBuilder builder() {
        return new ApprovalRequestBuilder();
    }

    public static class ApprovalRequestBuilder {
        private Long id;
        private String targetType;
        private Long targetId;
        private PortalUser requestedBy;
        private PortalUser approver;
        private String status = "PENDING";
        private String comments;
        private LocalDateTime requestedAt;
        private LocalDateTime decidedAt;

        public ApprovalRequestBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ApprovalRequestBuilder targetType(String targetType) {
            this.targetType = targetType;
            return this;
        }

        public ApprovalRequestBuilder targetId(Long targetId) {
            this.targetId = targetId;
            return this;
        }

        public ApprovalRequestBuilder requestedBy(PortalUser requestedBy) {
            this.requestedBy = requestedBy;
            return this;
        }

        public ApprovalRequestBuilder approver(PortalUser approver) {
            this.approver = approver;
            return this;
        }

        public ApprovalRequestBuilder status(String status) {
            this.status = status;
            return this;
        }

        public ApprovalRequestBuilder comments(String comments) {
            this.comments = comments;
            return this;
        }

        public ApprovalRequestBuilder requestedAt(LocalDateTime requestedAt) {
            this.requestedAt = requestedAt;
            return this;
        }

        public ApprovalRequestBuilder decidedAt(LocalDateTime decidedAt) {
            this.decidedAt = decidedAt;
            return this;
        }

        public ApprovalRequest build() {
            return new ApprovalRequest(id, targetType, targetId, requestedBy, approver, status, comments, requestedAt,
                    decidedAt);
        }
    }

    public Long getId() {
        return id;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public PortalUser getRequestedBy() {
        return requestedBy;
    }

    public PortalUser getApprover() {
        return approver;
    }

    public String getStatus() {
        return status;
    }

    public String getComments() {
        return comments;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public void setRequestedBy(PortalUser requestedBy) {
        this.requestedBy = requestedBy;
    }

    public void setApprover(PortalUser approver) {
        this.approver = approver;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }
}
