package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "customer_projects")
public class CustomerProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_uuid", nullable = false, updatable = false)
    private UUID projectUuid;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "project_phase", length = 100)
    private String projectPhase;

    @Column(length = 50)
    private String state;

    @Column(length = 50)
    private String district;

    @Column(name = "sqfeet", columnDefinition = "NUMERIC(10,2)")
    private BigDecimal sqfeet;

    @Column(name = "lead_id", nullable = true)
    private Long leadId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerUser customer;

    @Column(length = 255)
    private String code;

    @Column(name = "project_type", length = 255)
    private String projectType;

    @Column(name = "design_package", length = 255)
    private String designPackage;

    @Column(name = "is_design_agreement_signed", nullable = false)
    private Boolean isDesignAgreementSigned = false;

    @Column(name = "sq_feet")
    private Double sqFeet;

    // Project manager - has full control over all project tasks
    @Column(name = "project_manager_id")
    private Long projectManagerId;

    // Project members - SINGLE source of truth for team management
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProjectMember> projectMembers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", length = 50)
    private com.wd.api.model.enums.ContractType contractType = com.wd.api.model.enums.ContractType.TURNKEY;

    // Lead conversion tracking
    @Column(name = "converted_by_id")
    private Long convertedById;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (projectUuid == null) {
            projectUuid = UUID.randomUUID();
        }
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
        return customer != null ? customer.getId() : null;
    }

    public CustomerUser getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerUser customer) {
        this.customer = customer;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public Set<ProjectMember> getProjectMembers() {
        return projectMembers;
    }

    public void setProjectMembers(Set<ProjectMember> projectMembers) {
        this.projectMembers = projectMembers;
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

    public Long getProjectManagerId() {
        return projectManagerId;
    }

    public void setProjectManagerId(Long projectManagerId) {
        this.projectManagerId = projectManagerId;
    }

    public UUID getProjectUuid() {
        return projectUuid;
    }

    public void setProjectUuid(UUID projectUuid) {
        this.projectUuid = projectUuid;
    }

    public com.wd.api.model.enums.ContractType getContractType() {
        return contractType;
    }

    public void setContractType(com.wd.api.model.enums.ContractType contractType) {
        this.contractType = contractType;
    }
}
