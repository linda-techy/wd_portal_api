package com.wd.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class PartnershipApplicationRequest {
    
    // Primary Contact
    @NotBlank(message = "Contact name is required")
    private String contactName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String contactEmail;
    
    @NotBlank(message = "Phone number is required")
    private String contactPhone;
    
    private String designation;
    
    // Partnership Type
    @NotBlank(message = "Partnership type is required")
    private String partnershipType;
    
    // Business Information (conditional based on type)
    private String firmName;
    private String companyName;
    private String gstNumber;
    private String licenseNumber;
    private String reraNumber;
    private String cinNumber;
    private String ifscCode;
    private String employeeId;
    
    // Professional Details
    private Integer experience;
    private String specialization;
    private String portfolioLink;
    private String certifications;
    
    // Operational Details
    private String areaOfOperation;
    private String areasCovered;
    private String landTypes;
    private String materialsSupplied;
    private String businessSize;
    private String location;
    private String industry;
    private String projectType;
    private String projectScale;
    private String timeline;
    private Integer yearsOfPractice;
    private String areaServed;
    private String businessName;
    
    // Additional
    private String additionalContact;
    private String message;
    
    // Getters and Setters (all fields)
    
    public String getContactName() {
        return contactName;
    }
    
    public void setContactName(String contactName) {
        this.contactName = contactName;
    }
    
    public String getContactEmail() {
        return contactEmail;
    }
    
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
    
    public String getContactPhone() {
        return contactPhone;
    }
    
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
    
    public String getDesignation() {
        return designation;
    }
    
    public void setDesignation(String designation) {
        this.designation = designation;
    }
    
    public String getPartnershipType() {
        return partnershipType;
    }
    
    public void setPartnershipType(String partnershipType) {
        this.partnershipType = partnershipType;
    }
    
    public String getFirmName() {
        return firmName;
    }
    
    public void setFirmName(String firmName) {
        this.firmName = firmName;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public String getGstNumber() {
        return gstNumber;
    }
    
    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }
    
    public String getLicenseNumber() {
        return licenseNumber;
    }
    
    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }
    
    public String getReraNumber() {
        return reraNumber;
    }
    
    public void setReraNumber(String reraNumber) {
        this.reraNumber = reraNumber;
    }
    
    public String getCinNumber() {
        return cinNumber;
    }
    
    public void setCinNumber(String cinNumber) {
        this.cinNumber = cinNumber;
    }
    
    public String getIfscCode() {
        return ifscCode;
    }
    
    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    
    public Integer getExperience() {
        return experience;
    }
    
    public void setExperience(Integer experience) {
        this.experience = experience;
    }
    
    public String getSpecialization() {
        return specialization;
    }
    
    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }
    
    public String getPortfolioLink() {
        return portfolioLink;
    }
    
    public void setPortfolioLink(String portfolioLink) {
        this.portfolioLink = portfolioLink;
    }
    
    public String getCertifications() {
        return certifications;
    }
    
    public void setCertifications(String certifications) {
        this.certifications = certifications;
    }
    
    public String getAreaOfOperation() {
        return areaOfOperation;
    }
    
    public void setAreaOfOperation(String areaOfOperation) {
        this.areaOfOperation = areaOfOperation;
    }
    
    public String getAreasCovered() {
        return areasCovered;
    }
    
    public void setAreasCovered(String areasCovered) {
        this.areasCovered = areasCovered;
    }
    
    public String getLandTypes() {
        return landTypes;
    }
    
    public void setLandTypes(String landTypes) {
        this.landTypes = landTypes;
    }
    
    public String getMaterialsSupplied() {
        return materialsSupplied;
    }
    
    public void setMaterialsSupplied(String materialsSupplied) {
        this.materialsSupplied = materialsSupplied;
    }
    
    public String getBusinessSize() {
        return businessSize;
    }
    
    public void setBusinessSize(String businessSize) {
        this.businessSize = businessSize;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getIndustry() {
        return industry;
    }
    
    public void setIndustry(String industry) {
        this.industry = industry;
    }
    
    public String getProjectType() {
        return projectType;
    }
    
    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }
    
    public String getProjectScale() {
        return projectScale;
    }
    
    public void setProjectScale(String projectScale) {
        this.projectScale = projectScale;
    }
    
    public String getTimeline() {
        return timeline;
    }
    
    public void setTimeline(String timeline) {
        this.timeline = timeline;
    }
    
    public Integer getYearsOfPractice() {
        return yearsOfPractice;
    }
    
    public void setYearsOfPractice(Integer yearsOfPractice) {
        this.yearsOfPractice = yearsOfPractice;
    }
    
    public String getAreaServed() {
        return areaServed;
    }
    
    public void setAreaServed(String areaServed) {
        this.areaServed = areaServed;
    }
    
    public String getBusinessName() {
        return businessName;
    }
    
    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }
    
    public String getAdditionalContact() {
        return additionalContact;
    }
    
    public void setAdditionalContact(String additionalContact) {
        this.additionalContact = additionalContact;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}

