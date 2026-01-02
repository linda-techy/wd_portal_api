package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an interaction/communication with a lead
 * Tracks calls, emails, meetings, site visits, and other communications
 */
@Entity
@Table(name = "lead_interactions")
public class LeadInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_id", nullable = false)
    private Long leadId;

    @Column(name = "interaction_type", nullable = false, length = 50)
    private String interactionType; // CALL, EMAIL, MEETING, SITE_VISIT, WHATSAPP, SMS, OTHER

    @Column(name = "interaction_date", nullable = false)
    private LocalDateTime interactionDate = LocalDateTime.now();

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 100)
    private String outcome; // SCHEDULED_FOLLOWUP, QUOTE_SENT, NEEDS_INFO, NOT_INTERESTED, CONVERTED,
                            // HOT_LEAD, COLD_LEAD

    @Column(name = "next_action", length = 255)
    private String nextAction;

    @Column(name = "next_action_date")
    private LocalDateTime nextActionDate;

    @Column(length = 255)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON or key-value pairs for extra details

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (interactionDate == null) {
            interactionDate = LocalDateTime.now();
        }
    }

    // Constructors
    public LeadInteraction() {
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

    public String getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(String interactionType) {
        this.interactionType = interactionType;
    }

    public LocalDateTime getInteractionDate() {
        return interactionDate;
    }

    public void setInteractionDate(LocalDateTime interactionDate) {
        this.interactionDate = interactionDate;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    public LocalDateTime getNextActionDate() {
        return nextActionDate;
    }

    public void setNextActionDate(LocalDateTime nextActionDate) {
        this.nextActionDate = nextActionDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
