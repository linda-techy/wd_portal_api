package com.wd.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * DTO for converting a lead to a customer project
 * Enterprise-grade validation for construction management workflow
 */
public class LeadConversionRequest {
    
    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 255, message = "Project name must be between 3 and 255 characters")
    private String projectName;
    
    private String projectType; // 'RESIDENTIAL', 'COMMERCIAL', etc.
    
    private LocalDate startDate;
    
    private Long projectManagerId; // Optional assignment of PM
    
    private String location;
    
    private Long quotationId; // Optional Quote to convert from

    // Getters and Setters
    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public Long getProjectManagerId() {
        return projectManagerId;
    }

    public void setProjectManagerId(Long projectManagerId) {
        this.projectManagerId = projectManagerId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getQuotationId() {
        return quotationId;
    }

    public void setQuotationId(Long quotationId) {
        this.quotationId = quotationId;
    }
}
