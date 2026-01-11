package com.wd.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Lead Entity - Standardized for unified security model
 * 
 * Business Purpose (Construction Domain):
 * - Captures initial customer inquiries
 * - Tracks lead status, source, and priority
 * - Integrates with Lead Scoring system (COLD, WARM, HOT)
 * - Managed by Portal Users for assignment and conversion
 */
@Entity
@Table(name = "leads")
public class Lead extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lead_id")
    @JsonProperty("lead_id")
    private Long id;

    private String name;
    private String email;
    private String phone;

    @Column(name = "whatsapp_number")
    @JsonProperty("whatsapp_number")
    private String whatsappNumber;

    @Column(name = "lead_source")
    @JsonProperty("lead_source")
    private String leadSource; // website, whatsapp, calculator, referral, cold_call

    @Column(name = "lead_status")
    @JsonProperty("lead_status")
    private String leadStatus; // new, contacted, qualified, proposal_sent, converted, lost

    private String priority; // low, medium, high

    @Column(name = "customer_type")
    @JsonProperty("customer_type")
    private String customerType; // Individual, Business, Architect, Govt, etc.

    @Column(name = "project_type")
    @JsonProperty("project_type")
    private String projectType; // Residential, Commercial, Industrial, etc.

    @Column(columnDefinition = "TEXT")
    @JsonProperty("project_description")
    private String projectDescription;

    @Column(columnDefinition = "TEXT")
    private String requirements;
    private BigDecimal budget;

    @Column(name = "project_sqft_area")
    @JsonProperty("project_sqft_area")
    private BigDecimal projectSqftArea;

    @Column(name = "next_follow_up")
    @JsonProperty("next_follow_up")
    private LocalDateTime nextFollowUp;

    @Column(name = "last_contact_date")
    @JsonProperty("last_contact_date")
    private LocalDateTime lastContactDate;

    @Column(name = "assigned_team")
    @JsonProperty("assigned_team")
    private String assignedTeam;

    private String state;
    private String district;
    private String location;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "client_rating")
    @JsonProperty("client_rating")
    private Integer clientRating; // 1-5 stars

    @Column(name = "probability_to_win")
    @JsonProperty("probability_to_win")
    private Integer probabilityToWin; // 0-100

    @Column(columnDefinition = "TEXT")
    @JsonProperty("lost_reason")
    private String lostReason;

    @Column(name = "date_of_enquiry")
    @JsonProperty("date_of_enquiry")
    private LocalDate dateOfEnquiry;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_to_id")
    private PortalUser assignedTo;

    @Transient
    @JsonProperty("assigned_to_id")
    private Long assignedToId;

    // Lead Scoring System fields
    @Column(name = "score")
    private Integer score = 0;

    @Column(name = "score_category", length = 20)
    @JsonProperty("score_category")
    private String scoreCategory = "COLD";

    @Column(name = "last_scored_at")
    @JsonProperty("last_scored_at")
    private LocalDateTime lastScoredAt;

    @Column(name = "score_factors", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonProperty("score_factors")
    private String scoreFactors;

    @Column(name = "plot_area", columnDefinition = "NUMERIC(10,2)")
    @JsonProperty("plot_area")
    private BigDecimal plotArea;

    @Column(name = "floors")
    private Integer floors;

    @Column(name = "converted_by_id")
    @JsonProperty("converted_by_id")
    private Long convertedById;

    @Column(name = "converted_at")
    @JsonProperty("converted_at")
    private LocalDateTime convertedAt;

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

    public String getLeadSource() {
        return leadSource;
    }

    public void setLeadSource(String leadSource) {
        this.leadSource = leadSource;
    }

    public String getLeadStatus() {
        return leadStatus;
    }

    public void setLeadStatus(String leadStatus) {
        this.leadStatus = leadStatus;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public String getLostReason() {
        return lostReason;
    }

    public void setLostReason(String lostReason) {
        this.lostReason = lostReason;
    }

    public LocalDate getDateOfEnquiry() {
        return dateOfEnquiry;
    }

    public void setDateOfEnquiry(LocalDate dateOfEnquiry) {
        this.dateOfEnquiry = dateOfEnquiry;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PortalUser getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(PortalUser assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Long getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(Long assignedToId) {
        this.assignedToId = assignedToId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getScoreCategory() {
        return scoreCategory;
    }

    public void setScoreCategory(String scoreCategory) {
        this.scoreCategory = scoreCategory;
    }

    public LocalDateTime getLastScoredAt() {
        return lastScoredAt;
    }

    public void setLastScoredAt(LocalDateTime lastScoredAt) {
        this.lastScoredAt = lastScoredAt;
    }

    public String getScoreFactors() {
        return scoreFactors;
    }

    public void setScoreFactors(String scoreFactors) {
        this.scoreFactors = scoreFactors;
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
}
