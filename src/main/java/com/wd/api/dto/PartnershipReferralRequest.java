package com.wd.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PartnershipReferralRequest {
    
    @NotBlank(message = "Client name is required")
    private String clientName;
    
    @NotBlank(message = "Client email is required")
    @Email(message = "Valid email is required")
    private String clientEmail;
    
    @NotBlank(message = "Client phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String clientPhone;
    
    private String clientWhatsapp;
    
    // Customer type (individual, business, etc.)
    private String customerType;
    
    @NotBlank(message = "Project type is required")
    private String projectType;
    
    private String projectDescription;
    
    private BigDecimal estimatedBudget;
    
    private BigDecimal projectSqftArea;
    
    private String location;
    
    private String state;
    
    private String district;
    
    private String address;
    
    private String requirements;
    
    private String notes;
    
    // Lead tracking fields
    private String priority; // low, medium, high
    private String assignedTeam;
    private LocalDate dateOfEnquiry;
    private Integer clientRating; // 0-5
    private Integer probabilityToWin; // 0-100
    private LocalDateTime nextFollowUp;
    private LocalDateTime lastContactDate;
    
    // Partner information (for tracking)
    private String partnerId;
    private String partnerName;
    private String partnershipType;
    
    // Getters and Setters
    public String getClientName() {
        return clientName;
    }
    
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
    
    public String getClientEmail() {
        return clientEmail;
    }
    
    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }
    
    public String getClientPhone() {
        return clientPhone;
    }
    
    public void setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
    }
    
    public String getClientWhatsapp() {
        return clientWhatsapp;
    }
    
    public void setClientWhatsapp(String clientWhatsapp) {
        this.clientWhatsapp = clientWhatsapp;
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
    
    public BigDecimal getEstimatedBudget() {
        return estimatedBudget;
    }
    
    public void setEstimatedBudget(BigDecimal estimatedBudget) {
        this.estimatedBudget = estimatedBudget;
    }
    
    public BigDecimal getProjectSqftArea() {
        return projectSqftArea;
    }
    
    public void setProjectSqftArea(BigDecimal projectSqftArea) {
        this.projectSqftArea = projectSqftArea;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
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
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getRequirements() {
        return requirements;
    }
    
    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getPartnerId() {
        return partnerId;
    }
    
    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }
    
    public String getPartnerName() {
        return partnerName;
    }
    
    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }
    
    public String getPartnershipType() {
        return partnershipType;
    }
    
    public void setPartnershipType(String partnershipType) {
        this.partnershipType = partnershipType;
    }
    
    public String getCustomerType() {
        return customerType;
    }
    
    public void setCustomerType(String customerType) {
        this.customerType = customerType;
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
    
    public LocalDate getDateOfEnquiry() {
        return dateOfEnquiry;
    }
    
    public void setDateOfEnquiry(LocalDate dateOfEnquiry) {
        this.dateOfEnquiry = dateOfEnquiry;
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
}
