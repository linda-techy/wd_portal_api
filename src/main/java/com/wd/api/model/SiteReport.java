package com.wd.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wd.api.model.enums.ReportType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "site_reports")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class SiteReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "report_date")
    private LocalDateTime reportDate;

    @Column(length = 50)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private PortalUser submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 50)
    private ReportType reportType = ReportType.DAILY_PROGRESS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_visit_id")
    private SiteVisit siteVisit;

    @Column(length = 100)
    private String weather;

    @Column(name = "manpower_deployed")
    private Integer manpowerDeployed;

    @Column(name = "equipment_used", columnDefinition = "TEXT")
    private String equipmentUsed;

    @Column(name = "work_progress", columnDefinition = "TEXT")
    private String workProgress;

    // GPS/Location fields for accountability and tracking
    @Column(precision = 10, scale = 8)
    private Double latitude;

    @Column(precision = 11, scale = 8)
    private Double longitude;

    @Column(name = "location_accuracy", precision = 10, scale = 2)
    private Double locationAccuracy;

    @Column(name = "distance_from_project", precision = 10, scale = 2)
    private Double distanceFromProject;

    @OneToMany(mappedBy = "siteReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SiteReportPhoto> photos = new ArrayList<>();

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (reportDate == null) {
            reportDate = LocalDateTime.now();
        }
    }

    @Override
    @PreUpdate
    protected void onUpdate() {
        super.onUpdate();
    }

    // Getters and Setters
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDateTime reportDate) {
        this.reportDate = reportDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PortalUser getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(PortalUser submittedBy) {
        this.submittedBy = submittedBy;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public SiteVisit getSiteVisit() {
        return siteVisit;
    }

    public void setSiteVisit(SiteVisit siteVisit) {
        this.siteVisit = siteVisit;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public Integer getManpowerDeployed() {
        return manpowerDeployed;
    }

    public void setManpowerDeployed(Integer manpowerDeployed) {
        this.manpowerDeployed = manpowerDeployed;
    }

    public String getEquipmentUsed() {
        return equipmentUsed;
    }

    public void setEquipmentUsed(String equipmentUsed) {
        this.equipmentUsed = equipmentUsed;
    }

    public String getWorkProgress() {
        return workProgress;
    }

    public void setWorkProgress(String workProgress) {
        this.workProgress = workProgress;
    }

    public List<SiteReportPhoto> getPhotos() {
        return photos;
    }

    public void setPhotos(List<SiteReportPhoto> photos) {
        this.photos = photos;
    }

    public void addPhoto(SiteReportPhoto photo) {
        photos.add(photo);
        photo.setSiteReport(this);
    }

    public void removePhoto(SiteReportPhoto photo) {
        photos.remove(photo);
        photo.setSiteReport(null);
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLocationAccuracy() {
        return locationAccuracy;
    }

    public void setLocationAccuracy(Double locationAccuracy) {
        this.locationAccuracy = locationAccuracy;
    }

    public Double getDistanceFromProject() {
        return distanceFromProject;
    }

    public void setDistanceFromProject(Double distanceFromProject) {
        this.distanceFromProject = distanceFromProject;
    }
}
