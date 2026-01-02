package com.wd.api.model;

import jakarta.persistence.*;
import com.wd.api.model.enums.ProjectPhase;
import com.wd.api.model.enums.PermitStatus;
import com.wd.api.model.enums.ProjectStatus;
import com.wd.api.model.enums.ContractType;
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

    /**
     * Current phase of the project lifecycle
     * 
     * @see ProjectPhase for valid values
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "project_phase", length = 100)
    private ProjectPhase projectPhase = ProjectPhase.DESIGN;

    @Column(length = 50)
    private String state;

    @Column(length = 50)
    private String district;

    @Column(name = "sqfeet", columnDefinition = "NUMERIC(10,2)")
    private BigDecimal sqfeet;

    @Column(name = "lead_id", nullable = true)
    private Long leadId;

    @Column(name = "budget", precision = 15, scale = 2)
    private BigDecimal budget;

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

    /**
     * Project manager - has full control over all project tasks
     * Uses @ManyToOne relationship for proper entity management
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_manager_id")
    private PortalUser projectManager;

    // ==================== Collections (OneToMany Relationships)
    // ====================

    /**
     * Project members - SINGLE source of truth for team management
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProjectMember> projectMembers = new HashSet<>();

    /**
     * Tasks associated with this project
     */
    @OneToMany(mappedBy = "project", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<Task> tasks = new HashSet<>();

    /**
     * BOQ (Bill of Quantities) items for this project
     */
    @OneToMany(mappedBy = "project", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<BoqItem> boqItems = new HashSet<>();

    /**
     * Project documents (plans, drawings, contracts etc.)
     */
    @OneToMany(mappedBy = "project", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<ProjectDocument> documents = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", length = 50)
    private ContractType contractType = ContractType.TURNKEY;

    /**
     * Overall operational status of the project
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "project_status", length = 50)
    private ProjectStatus projectStatus = ProjectStatus.ACTIVE;

    // Lead conversion tracking
    @Column(name = "converted_by_id")
    private Long convertedById;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    // Link back to original lead source
    @Column(name = "converted_from_lead_id")
    private Long convertedFromLeadId;

    @Column(name = "plot_area", columnDefinition = "NUMERIC(10,2)")
    private BigDecimal plotArea;

    @Column(name = "floors")
    private Integer floors;

    @Column(length = 20)
    private String facing; // North, South, East, West, NE, NW, SE, SW

    /**
     * Construction permit/approval status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "permit_status", length = 50)
    private PermitStatus permitStatus;

    @Column(name = "project_description", columnDefinition = "TEXT")
    private String projectDescription;

    // ==================== Audit Trail Fields ====================

    /**
     * User who created this project (entity relationship for audit trail)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private PortalUser createdByUser;

    /**
     * User who last updated this project
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private PortalUser updatedByUser;

    /**
     * Soft delete timestamp - when project was deleted
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * User who soft-deleted this project
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_user_id")
    private PortalUser deletedByUser;

    /**
     * Version field for optimistic locking
     * Prevents lost updates in concurrent scenarios
     */
    @Version
    @Column(name = "version")
    private Long version;

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

    public ProjectPhase getProjectPhase() {
        return projectPhase;
    }

    public void setProjectPhase(ProjectPhase projectPhase) {
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

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
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

    /**
     * Get project manager entity
     */
    public PortalUser getProjectManager() {
        return projectManager;
    }

    /**
     * Set project manager entity
     */
    public void setProjectManager(PortalUser projectManager) {
        this.projectManager = projectManager;
    }

    /**
     * Helper method to get project manager ID
     * Maintains backward compatibility with existing code
     */
    public Long getProjectManagerId() {
        return projectManager != null ? projectManager.getId() : null;
    }

    /**
     * Helper method to set project manager by ID
     * For use when only the ID is available (e.g., from DTOs)
     * Note: This doesn't load the entity - use setProjectManager() for full
     * relationship
     */
    public void setProjectManagerId(Long projectManagerId) {
        // This is a helper for DTO mapping - actual entity loading should be done in
        // service layer
        if (projectManagerId == null) {
            this.projectManager = null;
        }
        // ID setting is handled in service layer via proper entity loading
    }

    public UUID getProjectUuid() {
        return projectUuid;
    }

    public void setProjectUuid(UUID projectUuid) {
        this.projectUuid = projectUuid;
    }

    /**
     * Get ContractType enum
     */
    public ContractType getContractType() {
        return contractType;
    }

    public void setContractType(ContractType contractType) {
        this.contractType = contractType;
    }

    public Long getConvertedById() {
        return convertedById;
    }

    public void setConvertedById(Long convertedById) {
        this.convertedById = convertedById;
    }

    public LocalDateTime getConvertedAt() {
        return convertedAt;
    }

    public void setConvertedAt(LocalDateTime convertedAt) {
        this.convertedAt = convertedAt;
    }

    public Long getConvertedFromLeadId() {
        return convertedFromLeadId;
    }

    public void setConvertedFromLeadId(Long convertedFromLeadId) {
        this.convertedFromLeadId = convertedFromLeadId;
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

    public String getFacing() {
        return facing;
    }

    public void setFacing(String facing) {
        this.facing = facing;
    }

    public PermitStatus getPermitStatus() {
        return permitStatus;
    }

    public void setPermitStatus(PermitStatus permitStatus) {
        this.permitStatus = permitStatus;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    // ==================== New Getters/Setters for Collections and Audit Fields
    // ====================

    public Set<Task> getTasks() {
        return tasks;
    }

    public void setTasks(Set<Task> tasks) {
        this.tasks = tasks;
    }

    public Set<BoqItem> getBoqItems() {
        return boqItems;
    }

    public void setBoqItems(Set<BoqItem> boqItems) {
        this.boqItems = boqItems;
    }

    public Set<ProjectDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(Set<ProjectDocument> documents) {
        this.documents = documents;
    }

    public ProjectStatus getProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(ProjectStatus projectStatus) {
        this.projectStatus = projectStatus;
    }

    public PortalUser getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(PortalUser createdByUser) {
        this.createdByUser = createdByUser;
    }

    public PortalUser getUpdatedByUser() {
        return updatedByUser;
    }

    public void setUpdatedByUser(PortalUser updatedByUser) {
        this.updatedByUser = updatedByUser;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public PortalUser getDeletedByUser() {
        return deletedByUser;
    }

    public void setDeletedByUser(PortalUser deletedByUser) {
        this.deletedByUser = deletedByUser;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // ==================== Convenience Methods ====================

    /**
     * Check if project is soft-deleted
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Check if project is active (not deleted)
     */
    public boolean isActive() {
        return deletedAt == null;
    }

    /**
     * Get count of tasks
     */
    public int getTaskCount() {
        return tasks != null ? tasks.size() : 0;
    }

    /**
     * Get count of BOQ items
     */
    public int getBoqItemCount() {
        return boqItems != null ? boqItems.size() : 0;
    }

    /**
     * Get count of documents
     */
    public int getDocumentCount() {
        return documents != null ? documents.size() : 0;
    }

    /**
     * Calculate total BOQ value
     */
    public BigDecimal getTotalBoqValue() {
        if (boqItems == null || boqItems.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return boqItems.stream()
                .map(BoqItem::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
