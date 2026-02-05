package com.wd.api.dto;

import com.wd.api.model.FeedbackResponse;

import java.time.LocalDateTime;

/**
 * DTO for FeedbackResponse entity.
 * Used for API responses to avoid exposing internal entity details.
 */
public class FeedbackResponseDto {
    private Long id;
    private Long formId;
    private String formTitle;
    private Long projectId;
    private String projectName;
    private Long customerId;
    private String customerName;
    private String responseData;
    private LocalDateTime submittedAt;

    public FeedbackResponseDto() {
    }

    public static FeedbackResponseDto fromEntity(FeedbackResponse entity) {
        FeedbackResponseDto dto = new FeedbackResponseDto();
        dto.setId(entity.getId());
        dto.setResponseData(entity.getResponseData());
        dto.setSubmittedAt(entity.getSubmittedAt());

        if (entity.getForm() != null) {
            dto.setFormId(entity.getForm().getId());
            dto.setFormTitle(entity.getForm().getTitle());
        }

        if (entity.getProject() != null) {
            dto.setProjectId(entity.getProject().getId());
            dto.setProjectName(entity.getProject().getName());
        }

        if (entity.getCustomer() != null) {
            dto.setCustomerId(entity.getCustomer().getId());
            dto.setCustomerName(entity.getCustomer().getFirstName() + " " + entity.getCustomer().getLastName());
        }

        return dto;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFormId() {
        return formId;
    }

    public void setFormId(Long formId) {
        this.formId = formId;
    }

    public String getFormTitle() {
        return formTitle;
    }

    public void setFormTitle(String formTitle) {
        this.formTitle = formTitle;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
