package com.wd.api.dto;

/**
 * Request DTO for check-out operation
 */
public class CheckOutRequest {
    private Double latitude;
    private Double longitude;
    private String notes;

    public CheckOutRequest() {
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
