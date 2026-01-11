package com.wd.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LeadCreateRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String phone;

    @JsonProperty("whatsapp_number")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "WhatsApp number must be 10-15 digits")
    private String whatsappNumber;

    @JsonProperty("customer_type")
    @NotBlank(message = "Customer type is required")
    private String customerType;

    @JsonProperty("project_type")
    private String projectType;

    @JsonProperty("project_description")
    private String projectDescription;

    private String requirements;
    private BigDecimal budget;

    @JsonProperty("project_sqft_area")
    private BigDecimal projectSqftArea;

    @JsonProperty("lead_status")
    @NotBlank(message = "Lead status is required")
    private String leadStatus;

    @JsonProperty("lead_source")
    @NotBlank(message = "Lead source is required")
    private String leadSource;

    @NotBlank(message = "Priority is required")
    private String priority;

    @JsonProperty("assigned_team")
    private String assignedTeam;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "District is required")
    private String district;
    private String location;

    private String notes;

    @JsonProperty("lost_reason")
    private String lostReason;

    @JsonProperty("client_rating")
    private Integer clientRating;

    @JsonProperty("probability_to_win")
    private Integer probabilityToWin;

    @JsonProperty("next_follow_up")
    private LocalDateTime nextFollowUp;

    @JsonProperty("last_contact_date")
    private LocalDateTime lastContactDate;

    @JsonProperty("date_of_enquiry")
    private String dateOfEnquiry; // Changed to String to handle ISO format

    private String address;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public BigDecimal getProjectSqftArea() {
        return projectSqftArea;
    }

    public void setProjectSqftArea(BigDecimal projectSqftArea) {
        this.projectSqftArea = projectSqftArea;
    }

    public String getLeadStatus() {
        return leadStatus;
    }

    public void setLeadStatus(String leadStatus) {
        this.leadStatus = leadStatus;
    }

    public String getLeadSource() {
        return leadSource;
    }

    public void setLeadSource(String leadSource) {
        this.leadSource = leadSource;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getAssignedTeam() {
        return assignedTeam;
    }

    public void setAssignedTeam(String assignedTeam) {
        this.assignedTeam = assignedTeam;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLostReason() {
        return lostReason;
    }

    public void setLostReason(String lostReason) {
        this.lostReason = lostReason;
    }

    public Integer getClientRating() {
        return clientRating;
    }

    public void setClientRating(Integer clientRating) {
        this.clientRating = clientRating;
    }

    public Integer getProbabilityToWin() {
        return probabilityToWin;
    }

    public void setProbabilityToWin(Integer probabilityToWin) {
        this.probabilityToWin = probabilityToWin;
    }

    public LocalDateTime getNextFollowUp() {
        return nextFollowUp;
    }

    public void setNextFollowUp(LocalDateTime nextFollowUp) {
        this.nextFollowUp = nextFollowUp;
    }

    public LocalDateTime getLastContactDate() {
        return lastContactDate;
    }

    public void setLastContactDate(LocalDateTime lastContactDate) {
        this.lastContactDate = lastContactDate;
    }

    public String getDateOfEnquiry() {
        return dateOfEnquiry;
    }

    public void setDateOfEnquiry(String dateOfEnquiry) {
        this.dateOfEnquiry = dateOfEnquiry;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @JsonProperty("assigned_to_id")
    private Long assignedToId;

    @JsonProperty("plot_area")
    private BigDecimal plotArea;

    private Integer floors;

    // Getters and Setters
    public Long getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(Long assignedToId) {
        this.assignedToId = assignedToId;
    }

    public BigDecimal getPlotArea() {
        return plotArea;
    }

    public void setPlotArea(BigDecimal plotArea) {
        this.plotArea = plotArea;
    }

    public Integer getFloors() {
        return floors;
    }

    public void setFloors(Integer floors) {
        this.floors = floors;
    }
}
