package com.wd.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wd.api.model.CustomerProject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CustomerProjectResponse {
    private Long id;
    private String name;
    private String location;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("project_phase")
    private String projectPhase;

    @JsonProperty("project_type")
    private String projectType;

    private String state;
    private String district;
    private BigDecimal sqfeet;

    @JsonProperty("lead_id")
    private Long leadId;

    @JsonProperty("customer_id")
    private Long customerId;

    @JsonProperty("team_members")
    private java.util.List<TeamMemberDTO> teamMembers;

    private String code;

    @JsonProperty("design_package")
    private String designPackage;

    @JsonProperty("is_design_agreement_signed")
    private Boolean isDesignAgreementSigned;

    @JsonProperty("sq_feet")
    private Double sqFeet;

    // Constructors
    public CustomerProjectResponse() {
    }

    public CustomerProjectResponse(CustomerProject project) {
        this.id = project.getId();
        this.name = project.getName();
        this.location = project.getLocation();
        this.startDate = project.getStartDate();
        this.endDate = project.getEndDate();
        this.createdAt = project.getCreatedAt();
        this.updatedAt = project.getUpdatedAt();

        this.createdBy = project.getCreatedBy();
        this.createdBy = project.getCreatedBy();
        this.projectPhase = project.getProjectPhase();
        this.projectType = project.getProjectType();
        this.state = project.getState();
        this.district = project.getDistrict();
        this.sqfeet = project.getSqfeet();
        this.leadId = project.getLeadId();
        this.customerId = project.getCustomerId();
        this.code = project.getCode();
        this.designPackage = project.getDesignPackage();
        this.isDesignAgreementSigned = project.getIsDesignAgreementSigned();

        this.sqFeet = project.getSqFeet();
        if (project.getProjectMembers() != null && !project.getProjectMembers().isEmpty()) {
            this.teamMembers = project.getProjectMembers().stream()
                    .map(pm -> {
                        if (pm.getPortalUser() != null) {
                            return new TeamMemberDTO(
                                    pm.getPortalUser().getId(),
                                    pm.getPortalUser().getFirstName(),
                                    pm.getPortalUser().getLastName(),
                                    pm.getPortalUser().getEmail(),
                                    "PORTAL");
                        } else if (pm.getCustomerUser() != null) {
                            return new TeamMemberDTO(
                                    pm.getCustomerUser().getId(),
                                    pm.getCustomerUser().getFirstName(),
                                    pm.getCustomerUser().getLastName(),
                                    pm.getCustomerUser().getEmail(),
                                    "CUSTOMER");
                        }
                        return null;
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getProjectPhase() {
        return projectPhase;
    }

    public void setProjectPhase(String projectPhase) {
        this.projectPhase = projectPhase;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public BigDecimal getSqfeet() {
        return sqfeet;
    }

    public void setSqfeet(BigDecimal sqfeet) {
        this.sqfeet = sqfeet;
    }

    public Long getLeadId() {
        return leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public java.util.List<TeamMemberDTO> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(java.util.List<TeamMemberDTO> teamMembers) {
        this.teamMembers = teamMembers;
    }

    public String getDesignPackage() {
        return designPackage;
    }

    public void setDesignPackage(String designPackage) {
        this.designPackage = designPackage;
    }

    public Boolean getIsDesignAgreementSigned() {
        return isDesignAgreementSigned;
    }

    public void setIsDesignAgreementSigned(Boolean isDesignAgreementSigned) {
        this.isDesignAgreementSigned = isDesignAgreementSigned;
    }

    public Double getSqFeet() {
        return sqFeet;
    }

    public void setSqFeet(Double sqFeet) {
        this.sqFeet = sqFeet;
    }
}
