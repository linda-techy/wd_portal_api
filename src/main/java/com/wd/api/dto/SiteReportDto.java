package com.wd.api.dto;

import com.wd.api.model.SiteReport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class SiteReportDto {
    private Long id;
    private Long projectId;
    private String projectName;
    private String title;
    private String description;
    private LocalDateTime reportDate;
    private String status;
    private String submittedByName;
    private LocalDateTime createdAt;
    private String reportType;
    private Long siteVisitId;
    private String weather;
    private Integer manpowerDeployed;
    private String equipmentUsed;
    private String workProgress;
    private Double latitude;
    private Double longitude;
    private Double locationAccuracy;
    private Double distanceFromProject;
    private List<SiteReportPhotoDto> photos;

    public SiteReportDto(SiteReport report) {
        this.id = report.getId();
        if (report.getProject() != null) {
            this.projectId = report.getProject().getId();
            this.projectName = report.getProject().getName();
        }
        this.title = report.getTitle();
        this.description = report.getDescription();
        this.reportDate = report.getReportDate();
        this.status = report.getStatus();

        if (report.getSubmittedBy() != null) {
            this.submittedByName = report.getSubmittedBy().getFirstName() + " " + report.getSubmittedBy().getLastName();
        }

        this.createdAt = report.getCreatedAt();
        this.reportType = report.getReportType() != null ? report.getReportType().name() : null;

        if (report.getSiteVisit() != null) {
            this.siteVisitId = report.getSiteVisit().getId();
        }

        this.weather = report.getWeather();
        this.manpowerDeployed = report.getManpowerDeployed();
        this.equipmentUsed = report.getEquipmentUsed();
        this.workProgress = report.getWorkProgress();

        this.latitude = report.getLatitude();
        this.longitude = report.getLongitude();
        this.locationAccuracy = report.getLocationAccuracy();
        this.distanceFromProject = report.getDistanceFromProject();

        if (report.getPhotos() != null) {
            this.photos = report.getPhotos().stream()
                    .map(SiteReportPhotoDto::new)
                    .sorted((p1, p2) -> Integer.compare(p1.getDisplayOrder(), p2.getDisplayOrder()))
                    .collect(Collectors.toList());
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
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

    public String getSubmittedByName() {
        return submittedByName;
    }

    public void setSubmittedByName(String submittedByName) {
        this.submittedByName = submittedByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public Long getSiteVisitId() {
        return siteVisitId;
    }

    public void setSiteVisitId(Long siteVisitId) {
        this.siteVisitId = siteVisitId;
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

    public List<SiteReportPhotoDto> getPhotos() {
        return photos;
    }

    public void setPhotos(List<SiteReportPhotoDto> photos) {
        this.photos = photos;
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
