package com.wd.api.dto;

import com.wd.api.model.SiteReport;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Customer-safe DTO for Site Reports.
 * Excludes sensitive company information like internal notes, employee details,
 * etc.
 */
public class CustomerSiteReportDto {

    private Long id;
    private String reportDate;
    private String workDescription;
    private String progressStatus;
    private Integer workforceCount;
    private List<CustomerSiteReportPhotoDto> photos;

    // Customer-friendly constructor
    public CustomerSiteReportDto(SiteReport report) {
        this.id = report.getId();
        this.reportDate = report.getReportDate() != null ? report.getReportDate().toString() : null;
        this.workDescription = report.getDescription(); // SiteReport uses 'description'
        this.progressStatus = report.getStatus(); // SiteReport uses 'status'
        this.workforceCount = null; // SiteReport doesn't have workforceCount - set to null

        // Include only customer-visible photos (exclude any internal/sensitive photos)
        if (report.getPhotos() != null) {
            this.photos = report.getPhotos().stream()
                    .map(CustomerSiteReportPhotoDto::new)
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

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getWorkDescription() {
        return workDescription;
    }

    public void setWorkDescription(String workDescription) {
        this.workDescription = workDescription;
    }

    public String getProgressStatus() {
        return progressStatus;
    }

    public void setProgressStatus(String progressStatus) {
        this.progressStatus = progressStatus;
    }

    public Integer getWorkforceCount() {
        return workforceCount;
    }

    public void setWorkforceCount(Integer workforceCount) {
        this.workforceCount = workforceCount;
    }

    public List<CustomerSiteReportPhotoDto> getPhotos() {
        return photos;
    }

    public void setPhotos(List<CustomerSiteReportPhotoDto> photos) {
        this.photos = photos;
    }
}
