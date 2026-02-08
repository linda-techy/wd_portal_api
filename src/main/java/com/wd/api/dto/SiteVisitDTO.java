package com.wd.api.dto;

import java.time.LocalDateTime;

/**
 * DTO for Site Visit data transfer
 */
public class SiteVisitDTO {
    private Long id;
    private Long projectId;
    private String projectName;
    private Long visitedById;
    private String visitedByName;
    private LocalDateTime visitDate;
    private String notes;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Double checkInLatitude;
    private Double checkInLongitude;
    private Double checkOutLatitude;
    private Double checkOutLongitude;
    private String visitType;
    private String visitStatus;
    private Integer durationMinutes;
    private String formattedDuration;
    private String checkOutNotes;
    private LocalDateTime createdAt;
    private Double distanceFromProjectCheckIn;
    private Double distanceFromProjectCheckOut;

    public SiteVisitDTO() {
    }

    // Builder pattern
    public static SiteVisitDTOBuilder builder() {
        return new SiteVisitDTOBuilder();
    }

    public static class SiteVisitDTOBuilder {
        private final SiteVisitDTO dto = new SiteVisitDTO();

        public SiteVisitDTOBuilder id(Long id) {
            dto.id = id;
            return this;
        }

        public SiteVisitDTOBuilder projectId(Long projectId) {
            dto.projectId = projectId;
            return this;
        }

        public SiteVisitDTOBuilder projectName(String projectName) {
            dto.projectName = projectName;
            return this;
        }

        public SiteVisitDTOBuilder visitedById(Long visitedById) {
            dto.visitedById = visitedById;
            return this;
        }

        public SiteVisitDTOBuilder visitedByName(String visitedByName) {
            dto.visitedByName = visitedByName;
            return this;
        }

        public SiteVisitDTOBuilder visitDate(LocalDateTime visitDate) {
            dto.visitDate = visitDate;
            return this;
        }

        public SiteVisitDTOBuilder notes(String notes) {
            dto.notes = notes;
            return this;
        }

        public SiteVisitDTOBuilder checkInTime(LocalDateTime checkInTime) {
            dto.checkInTime = checkInTime;
            return this;
        }

        public SiteVisitDTOBuilder checkOutTime(LocalDateTime checkOutTime) {
            dto.checkOutTime = checkOutTime;
            return this;
        }

        public SiteVisitDTOBuilder checkInLatitude(Double lat) {
            dto.checkInLatitude = lat;
            return this;
        }

        public SiteVisitDTOBuilder checkInLongitude(Double lng) {
            dto.checkInLongitude = lng;
            return this;
        }

        public SiteVisitDTOBuilder checkOutLatitude(Double lat) {
            dto.checkOutLatitude = lat;
            return this;
        }

        public SiteVisitDTOBuilder checkOutLongitude(Double lng) {
            dto.checkOutLongitude = lng;
            return this;
        }

        public SiteVisitDTOBuilder visitType(String visitType) {
            dto.visitType = visitType;
            return this;
        }

        public SiteVisitDTOBuilder visitStatus(String visitStatus) {
            dto.visitStatus = visitStatus;
            return this;
        }

        public SiteVisitDTOBuilder durationMinutes(Integer durationMinutes) {
            dto.durationMinutes = durationMinutes;
            return this;
        }

        public SiteVisitDTOBuilder formattedDuration(String formattedDuration) {
            dto.formattedDuration = formattedDuration;
            return this;
        }

        public SiteVisitDTOBuilder checkOutNotes(String checkOutNotes) {
            dto.checkOutNotes = checkOutNotes;
            return this;
        }

        public SiteVisitDTOBuilder createdAt(LocalDateTime createdAt) {
            dto.createdAt = createdAt;
            return this;
        }

        public SiteVisitDTOBuilder distanceFromProjectCheckIn(Double distance) {
            dto.distanceFromProjectCheckIn = distance;
            return this;
        }

        public SiteVisitDTOBuilder distanceFromProjectCheckOut(Double distance) {
            dto.distanceFromProjectCheckOut = distance;
            return this;
        }

        public SiteVisitDTO build() {
            return dto;
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

    public Long getVisitedById() {
        return visitedById;
    }

    public void setVisitedById(Long visitedById) {
        this.visitedById = visitedById;
    }

    public String getVisitedByName() {
        return visitedByName;
    }

    public void setVisitedByName(String visitedByName) {
        this.visitedByName = visitedByName;
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

    public String getVisitType() {
        return visitType;
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public String getVisitStatus() {
        return visitStatus;
    }

    public void setVisitStatus(String visitStatus) {
        this.visitStatus = visitStatus;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getFormattedDuration() {
        return formattedDuration;
    }

    public void setFormattedDuration(String formattedDuration) {
        this.formattedDuration = formattedDuration;
    }

    public String getCheckOutNotes() {
        return checkOutNotes;
    }

    public void setCheckOutNotes(String checkOutNotes) {
        this.checkOutNotes = checkOutNotes;
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
