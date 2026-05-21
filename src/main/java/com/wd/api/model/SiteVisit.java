package com.wd.api.model;

import com.wd.api.model.enums.VisitStatus;
import com.wd.api.model.enums.VisitType;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Tracks site visits by employees (Site Engineers, Project Managers, etc.)
 * Includes GPS-based check-in/check-out for duty verification
 */
@Entity
@Table(name = "site_visits")
public class SiteVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(name = "visit_date")
    private LocalDateTime visitDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visited_by")
    private PortalUser visitedBy;

    // ==================== Check-In/Check-Out Fields ====================

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "check_in_latitude")
    private Double checkInLatitude;

    @Column(name = "check_in_longitude")
    private Double checkInLongitude;

    @Column(name = "check_out_latitude")
    private Double checkOutLatitude;

    @Column(name = "check_out_longitude")
    private Double checkOutLongitude;

    @Column(name = "distance_from_project_checkin")
    private Double distanceFromProjectCheckIn;

    @Column(name = "distance_from_project_checkout")
    private Double distanceFromProjectCheckOut;

    @Enumerated(EnumType.STRING)
    @Column(name = "visit_type", length = 50)
    private VisitType visitType = VisitType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "visit_status", length = 50)
    private VisitStatus visitStatus = VisitStatus.PENDING;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "check_out_notes", columnDefinition = "TEXT")
    private String checkOutNotes;

    @Column(name = "purpose", length = 50)
    private String purpose;

    // Force-close audit (V151). Populated when an admin closes someone else's
    // stuck visit, bypassing the GPS check. Null for normal check-outs.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "force_closed_by_user_id")
    private PortalUser forceClosedBy;

    @Column(name = "force_close_reason", columnDefinition = "TEXT")
    private String forceCloseReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ==================== Lifecycle Callbacks ====================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (visitDate == null) {
            visitDate = LocalDateTime.now();
        }
    }

    // ==================== Business Methods ====================

    /**
     * Perform check-in with GPS coordinates
     */
    public void checkIn(Double latitude, Double longitude) {
        if (this.visitStatus != VisitStatus.PENDING) {
            throw new IllegalStateException("Cannot check-in: current status is " + this.visitStatus);
        }
        this.checkInTime = LocalDateTime.now();
        this.checkInLatitude = latitude;
        this.checkInLongitude = longitude;
        this.visitStatus = VisitStatus.CHECKED_IN;
    }

    /**
     * Perform check-out with GPS coordinates and notes
     */
    public void checkOut(Double latitude, Double longitude, String notes) {
        if (this.visitStatus != VisitStatus.CHECKED_IN) {
            throw new IllegalStateException("Cannot check-out: current status is " + this.visitStatus);
        }
        this.checkOutTime = LocalDateTime.now();
        this.checkOutLatitude = latitude;
        this.checkOutLongitude = longitude;
        this.checkOutNotes = notes;
        this.visitStatus = VisitStatus.CHECKED_OUT;

        // Calculate duration
        if (this.checkInTime != null) {
            Duration duration = Duration.between(this.checkInTime, this.checkOutTime);
            this.durationMinutes = (int) duration.toMinutes();
        }
    }

    /**
     * Admin force-close. Bypasses the GPS geofence — used to unstick visits
     * (lost phone, dead GPS, geofence policy change, etc.). Caller is
     * responsible for the @PreAuthorize check. The visit is marked
     * CHECKED_OUT and the audit fields (forceClosedBy, forceCloseReason) are
     * populated so the action is traceable forever.
     */
    public void forceClose(PortalUser admin, String reason) {
        if (this.visitStatus != VisitStatus.CHECKED_IN) {
            throw new IllegalStateException(
                "Cannot force-close: visit is " + this.visitStatus + " (only CHECKED_IN visits can be force-closed)");
        }
        this.checkOutTime = LocalDateTime.now();
        this.visitStatus = VisitStatus.CHECKED_OUT;
        this.forceClosedBy = admin;
        this.forceCloseReason = reason;
        // Distinguish from a real check-out: leave checkOutLatitude/Longitude null.
        // Append to check-out notes so the timeline view shows the reason inline.
        String stamp = "[FORCE-CLOSED by "
                + (admin != null ? (admin.getFirstName() + " " + admin.getLastName()).trim() : "system")
                + "]: " + (reason == null ? "" : reason);
        this.checkOutNotes = (this.checkOutNotes == null || this.checkOutNotes.isBlank())
                ? stamp
                : this.checkOutNotes + "\n" + stamp;
        if (this.checkInTime != null) {
            this.durationMinutes = (int) Duration.between(this.checkInTime, this.checkOutTime).toMinutes();
        }
    }

    /**
     * Check if visit is currently active (checked in but not out)
     */
    public boolean isActive() {
        return this.visitStatus == VisitStatus.CHECKED_IN;
    }

    /**
     * Get formatted duration string
     */
    public String getFormattedDuration() {
        if (durationMinutes == null)
            return "";
        int hours = durationMinutes / 60;
        int mins = durationMinutes % 60;
        return String.format("%dh %dm", hours, mins);
    }

    // ==================== Getters and Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerProject getProject() {
        return project;
    }

    public void setProject(CustomerProject project) {
        this.project = project;
    }

    public LocalDateTime getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDateTime visitDate) {
        this.visitDate = visitDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public PortalUser getVisitedBy() {
        return visitedBy;
    }

    public void setVisitedBy(PortalUser visitedBy) {
        this.visitedBy = visitedBy;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public Double getCheckInLatitude() {
        return checkInLatitude;
    }

    public void setCheckInLatitude(Double checkInLatitude) {
        this.checkInLatitude = checkInLatitude;
    }

    public Double getCheckInLongitude() {
        return checkInLongitude;
    }

    public void setCheckInLongitude(Double checkInLongitude) {
        this.checkInLongitude = checkInLongitude;
    }

    public Double getCheckOutLatitude() {
        return checkOutLatitude;
    }

    public void setCheckOutLatitude(Double checkOutLatitude) {
        this.checkOutLatitude = checkOutLatitude;
    }

    public Double getCheckOutLongitude() {
        return checkOutLongitude;
    }

    public void setCheckOutLongitude(Double checkOutLongitude) {
        this.checkOutLongitude = checkOutLongitude;
    }

    public VisitType getVisitType() {
        return visitType;
    }

    public void setVisitType(VisitType visitType) {
        this.visitType = visitType;
    }

    public VisitStatus getVisitStatus() {
        return visitStatus;
    }

    public void setVisitStatus(VisitStatus visitStatus) {
        this.visitStatus = visitStatus;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getCheckOutNotes() {
        return checkOutNotes;
    }

    public void setCheckOutNotes(String checkOutNotes) {
        this.checkOutNotes = checkOutNotes;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public PortalUser getForceClosedBy() {
        return forceClosedBy;
    }

    public void setForceClosedBy(PortalUser forceClosedBy) {
        this.forceClosedBy = forceClosedBy;
    }

    public String getForceCloseReason() {
        return forceCloseReason;
    }

    public void setForceCloseReason(String forceCloseReason) {
        this.forceCloseReason = forceCloseReason;
    }

    public boolean isForceClosed() {
        return forceClosedBy != null || forceCloseReason != null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Double getDistanceFromProjectCheckIn() {
        return distanceFromProjectCheckIn;
    }

    public void setDistanceFromProjectCheckIn(Double distanceFromProjectCheckIn) {
        this.distanceFromProjectCheckIn = distanceFromProjectCheckIn;
    }

    public Double getDistanceFromProjectCheckOut() {
        return distanceFromProjectCheckOut;
    }

    public void setDistanceFromProjectCheckOut(Double distanceFromProjectCheckOut) {
        this.distanceFromProjectCheckOut = distanceFromProjectCheckOut;
    }
}
