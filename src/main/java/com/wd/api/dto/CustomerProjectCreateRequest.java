package com.wd.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CustomerProjectCreateRequest {
    private String name;
    private String location;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    private Double progress;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("project_phase")
    private String projectPhase;

    private String state;
    private String district;

    private BigDecimal sqfeet;

    // Note: leadId field doesn't have @JsonProperty - using @JsonSetter on setter
    // instead
    private Long leadId;

    @JsonProperty("team_members")
    private java.util.List<TeamMemberSelectionDTO> teamMembers;

    private String code;

    // Constructors
    public CustomerProjectCreateRequest() {
    }

    // Getters and Setters
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

    public Double getProgress() {
        return progress;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
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

    // Custom setter to handle both BigDecimal and Number types from JSON
    @JsonSetter("sqfeet")
    public void setSqfeet(Object sqfeet) {
        if (sqfeet == null) {
            this.sqfeet = null;
        } else if (sqfeet instanceof BigDecimal) {
            this.sqfeet = (BigDecimal) sqfeet;
        } else if (sqfeet instanceof Number) {
            this.sqfeet = BigDecimal.valueOf(((Number) sqfeet).doubleValue());
        } else if (sqfeet instanceof String) {
            String str = (String) sqfeet;
            if (str.trim().isEmpty()) {
                this.sqfeet = null;
            } else {
                this.sqfeet = new BigDecimal(str);
            }
        } else {
            this.sqfeet = null;
        }
    }

    // Regular setter for BigDecimal (for programmatic use)
    public void setSqfeetValue(BigDecimal sqfeet) {
        this.sqfeet = sqfeet;
    }

    public Long getLeadId() {
        return leadId;
    }

    // Custom setter to handle both Long and Integer types from JSON
    @JsonSetter("lead_id")
    public void setLeadId(Object leadId) {
        if (leadId == null) {
            this.leadId = null;
        } else if (leadId instanceof Long) {
            this.leadId = (Long) leadId;
        } else if (leadId instanceof Integer) {
            this.leadId = ((Integer) leadId).longValue();
        } else if (leadId instanceof Number) {
            this.leadId = ((Number) leadId).longValue();
        } else if (leadId instanceof String) {
            String str = (String) leadId;
            if (str.trim().isEmpty()) {
                this.leadId = null;
            } else {
                try {
                    this.leadId = Long.parseLong(str);
                } catch (NumberFormatException e) {
                    this.leadId = null;
                }
            }
        } else {
            this.leadId = null;
        }
    }

    // Regular setter for Long (for programmatic use)
    public void setLeadIdValue(Long leadId) {
        this.leadId = leadId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public java.util.List<TeamMemberSelectionDTO> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(java.util.List<TeamMemberSelectionDTO> teamMembers) {
        this.teamMembers = teamMembers;
    }
}
