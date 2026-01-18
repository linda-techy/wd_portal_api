package com.wd.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entity representing the history of lead score changes
 * Tracks all score changes for audit trail and business intelligence
 * 
 * Business Purpose (Construction Domain):
 * - Audit trail of lead quality assessments
 * - Track score changes over time for analysis
 * - Monitor when leads become "HOT", "WARM", or "COLD"
 * - Support lead scoring algorithm refinement
 */
@Entity
@Table(name = "lead_score_history")
public class LeadScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_id", nullable = false)
    private Long leadId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", insertable = false, updatable = false)
    private Lead lead;

    @Column(name = "previous_score")
    private Integer previousScore;

    @Column(name = "new_score", nullable = false)
    private Integer newScore;

    @Column(name = "previous_category", length = 20)
    private String previousCategory;

    @Column(name = "new_category", length = 20, nullable = false)
    private String newCategory;

    @Column(name = "score_factors", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String scoreFactors;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "scored_at", nullable = false, updatable = false)
    private LocalDateTime scoredAt = LocalDateTime.now();

    @Column(name = "scored_by_id")
    private Long scoredById;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scored_by_id", insertable = false, updatable = false)
    private PortalUser scoredBy;

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (scoredAt == null) {
            scoredAt = LocalDateTime.now();
        }
    }

    // Constructors
    public LeadScoreHistory() {
    }

    public LeadScoreHistory(Long leadId, Integer previousScore, Integer newScore,
                          String previousCategory, String newCategory, Long scoredById) {
        this.leadId = leadId;
        this.previousScore = previousScore;
        this.newScore = newScore;
        this.previousCategory = previousCategory;
        this.newCategory = newCategory;
        this.scoredById = scoredById;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLeadId() {
        return leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }

    public Lead getLead() {
        return lead;
    }

    public void setLead(Lead lead) {
        this.lead = lead;
    }

    public Integer getPreviousScore() {
        return previousScore;
    }

    public void setPreviousScore(Integer previousScore) {
        this.previousScore = previousScore;
    }

    public Integer getNewScore() {
        return newScore;
    }

    public void setNewScore(Integer newScore) {
        this.newScore = newScore;
    }

    public String getPreviousCategory() {
        return previousCategory;
    }

    public void setPreviousCategory(String previousCategory) {
        this.previousCategory = previousCategory;
    }

    public String getNewCategory() {
        return newCategory;
    }

    public void setNewCategory(String newCategory) {
        this.newCategory = newCategory;
    }

    public String getScoreFactors() {
        return scoreFactors;
    }

    public void setScoreFactors(String scoreFactors) {
        this.scoreFactors = scoreFactors;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getScoredAt() {
        return scoredAt;
    }

    public void setScoredAt(LocalDateTime scoredAt) {
        this.scoredAt = scoredAt;
    }

    public Long getScoredById() {
        return scoredById;
    }

    public void setScoredById(Long scoredById) {
        this.scoredById = scoredById;
    }

    public PortalUser getScoredBy() {
        return scoredBy;
    }

    public void setScoredBy(PortalUser scoredBy) {
        this.scoredBy = scoredBy;
    }
}
