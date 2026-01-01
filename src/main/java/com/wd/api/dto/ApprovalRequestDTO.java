package com.wd.api.dto;

import java.time.LocalDateTime;

public class ApprovalRequestDTO {
    private Long id;
    private String targetType;
    private Long targetId;
    private String targetName; // Optional display name
    private Long requestedById;
    private String requestedByName;
    private Long approverId;
    private String approverName;
    private String status;
    private String comments;
    private LocalDateTime requestedAt;
    private LocalDateTime decidedAt;

    public ApprovalRequestDTO() {
    }

    public ApprovalRequestDTO(Long id, String targetType, Long targetId, String targetName, Long requestedById,
            String requestedByName, Long approverId, String approverName, String status, String comments,
            LocalDateTime requestedAt, LocalDateTime decidedAt) {
        this.id = id;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetName = targetName;
        this.requestedById = requestedById;
        this.requestedByName = requestedByName;
        this.approverId = approverId;
        this.approverName = approverName;
        this.status = status;
        this.comments = comments;
        this.requestedAt = requestedAt;
        this.decidedAt = decidedAt;
    }

    public static ApprovalRequestDTOBuilder builder() {
        return new ApprovalRequestDTOBuilder();
    }

    public static class ApprovalRequestDTOBuilder {
        private Long id;
        private String targetType;
        private Long targetId;
        private String targetName;
        private Long requestedById;
        private String requestedByName;
        private Long approverId;
        private String approverName;
        private String status;
        private String comments;
        private LocalDateTime requestedAt;
        private LocalDateTime decidedAt;

        public ApprovalRequestDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ApprovalRequestDTOBuilder targetType(String targetType) {
            this.targetType = targetType;
            return this;
        }

        public ApprovalRequestDTOBuilder targetId(Long targetId) {
            this.targetId = targetId;
            return this;
        }

        public ApprovalRequestDTOBuilder targetName(String targetName) {
            this.targetName = targetName;
            return this;
        }

        public ApprovalRequestDTOBuilder requestedById(Long requestedById) {
            this.requestedById = requestedById;
            return this;
        }

        public ApprovalRequestDTOBuilder requestedByName(String requestedByName) {
            this.requestedByName = requestedByName;
            return this;
        }

        public ApprovalRequestDTOBuilder approverId(Long approverId) {
            this.approverId = approverId;
            return this;
        }

        public ApprovalRequestDTOBuilder approverName(String approverName) {
            this.approverName = approverName;
            return this;
        }

        public ApprovalRequestDTOBuilder status(String status) {
            this.status = status;
            return this;
        }

        public ApprovalRequestDTOBuilder comments(String comments) {
            this.comments = comments;
            return this;
        }

        public ApprovalRequestDTOBuilder requestedAt(LocalDateTime requestedAt) {
            this.requestedAt = requestedAt;
            return this;
        }

        public ApprovalRequestDTOBuilder decidedAt(LocalDateTime decidedAt) {
            this.decidedAt = decidedAt;
            return this;
        }

        public ApprovalRequestDTO build() {
            return new ApprovalRequestDTO(id, targetType, targetId, targetName, requestedById, requestedByName,
                    approverId, approverName, status, comments, requestedAt, decidedAt);
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

    public String getTargetName() {
        return targetName;
    }

    public Long getRequestedById() {
        return requestedById;
    }

    public String getRequestedByName() {
        return requestedByName;
    }

    public Long getApproverId() {
        return approverId;
    }

    public String getApproverName() {
        return approverName;
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

    public void setId(Long id) {
        this.id = id;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public void setRequestedById(Long requestedById) {
        this.requestedById = requestedById;
    }

    public void setRequestedByName(String requestedByName) {
        this.requestedByName = requestedByName;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
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
