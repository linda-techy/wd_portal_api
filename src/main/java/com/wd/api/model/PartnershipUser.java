package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "partnership_users")
public class PartnershipUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Login Credentials
    @Column(name = "phone", unique = true, nullable = false, length = 15)
    private String phone;
    
    @Column(name = "email", unique = true, nullable = false)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    // Personal Information
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    private String designation;
    
    // Partnership Details
    @Column(name = "partnership_type", nullable = false, length = 50)
    private String partnershipType;
    
    // Business Information
    @Column(name = "firm_name")
    private String firmName;
    
    @Column(name = "company_name")
    private String companyName;
    
    // Verification Documents
    @Column(name = "gst_number", length = 20)
    private String gstNumber;
    
    @Column(name = "license_number", length = 100)
    private String licenseNumber;
    
    @Column(name = "rera_number", length = 100)
    private String reraNumber;
    
    @Column(name = "cin_number", length = 50)
    private String cinNumber;
    
    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;
    
    @Column(name = "employee_id", length = 100)
    private String employeeId;
    
    // Professional Details
    private Integer experience;
    
    private String specialization;
    
    @Column(name = "portfolio_link", length = 500)
    private String portfolioLink;
    
    @Column(columnDefinition = "TEXT")
    private String certifications;
    
    // Operational Details
    @Column(name = "area_of_operation")
    private String areaOfOperation;
    
    @Column(name = "areas_covered")
    private String areasCovered;
    
    @Column(name = "land_types")
    private String landTypes;
    
    @Column(name = "materials_supplied", length = 500)
    private String materialsSupplied;
    
    @Column(name = "business_size", length = 50)
    private String businessSize;
    
    private String location;
    
    private String industry;
    
    @Column(name = "project_type", length = 100)
    private String projectType;
    
    @Column(name = "project_scale", length = 50)
    private String projectScale;
    
    private String timeline;
    
    @Column(name = "years_of_practice")
    private Integer yearsOfPractice;
    
    @Column(name = "area_served")
    private String areaServed;
    
    @Column(name = "business_name")
    private String businessName;
    
    // Additional Information
    @Column(name = "additional_contact")
    private String additionalContact;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    // Account Status
    @Column(length = 20)
    private String status = "pending";
    
    // Timestamps
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    // Metadata
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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
    
    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
    
    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}

