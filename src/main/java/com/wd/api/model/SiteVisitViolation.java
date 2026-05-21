package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Audit record for a failed (out-of-geofence) site-visit check-in or check-out attempt.
 * Rows here are append-only audit data; they are NEVER exposed to customers.
 * See V150__site_visit_violations.sql for the table contract.
 */
@Entity
@Table(name = "site_visit_violations")
public class SiteVisitViolation {

    public enum AttemptType { CHECK_IN, CHECK_OUT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private PortalUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_type", nullable = false, length = 20)
    private AttemptType attemptType;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    @Column(name = "attempted_latitude", nullable = false)
    private Double attemptedLatitude;

    @Column(name = "attempted_longitude", nullable = false)
    private Double attemptedLongitude;

    @Column(name = "project_latitude")
    private Double projectLatitude;

    @Column(name = "project_longitude")
    private Double projectLongitude;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "allowed_radius_km", nullable = false)
    private Double allowedRadiusKm;

    @Column(name = "visit_id")
    private Long visitId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (attemptedAt == null) attemptedAt = LocalDateTime.now();
        createdAt = LocalDateTime.now();
    }

    // ── getters / setters ─────────────────────────────────────────────────
    public Long getId() { return id; }
    public CustomerProject getProject() { return project; }
    public void setProject(CustomerProject project) { this.project = project; }
    public PortalUser getUser() { return user; }
    public void setUser(PortalUser user) { this.user = user; }
    public AttemptType getAttemptType() { return attemptType; }
    public void setAttemptType(AttemptType attemptType) { this.attemptType = attemptType; }
    public LocalDateTime getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(LocalDateTime attemptedAt) { this.attemptedAt = attemptedAt; }
    public Double getAttemptedLatitude() { return attemptedLatitude; }
    public void setAttemptedLatitude(Double v) { this.attemptedLatitude = v; }
    public Double getAttemptedLongitude() { return attemptedLongitude; }
    public void setAttemptedLongitude(Double v) { this.attemptedLongitude = v; }
    public Double getProjectLatitude() { return projectLatitude; }
    public void setProjectLatitude(Double v) { this.projectLatitude = v; }
    public Double getProjectLongitude() { return projectLongitude; }
    public void setProjectLongitude(Double v) { this.projectLongitude = v; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double v) { this.distanceKm = v; }
    public Double getAllowedRadiusKm() { return allowedRadiusKm; }
    public void setAllowedRadiusKm(Double v) { this.allowedRadiusKm = v; }
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
