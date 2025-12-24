package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Column(name = "progress")
    private Double progress;

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

    @Column(name = "customer_id", nullable = true)
    private Long customerId;

    @Column(length = 255)
    private String code;

    @Column(name = "project_type", length = 255)
    private String projectType;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "customer_project_team_members", joinColumns = @JoinColumn(name = "project_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private java.util.Set<User> teamMembers = new java.util.HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<ProjectMember> projectMembers = new java.util.HashSet<>();

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

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public java.util.Set<User> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(java.util.Set<User> teamMembers) {
        this.teamMembers = teamMembers;
    }

    public java.util.Set<ProjectMember> getProjectMembers() {
        return projectMembers;
    }

    public void setProjectMembers(java.util.Set<ProjectMember> projectMembers) {
        this.projectMembers = projectMembers;
    }
}
