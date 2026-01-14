package com.wd.api.dto;

/**
 * DTO for District master data
 */
public class DistrictDTO {
    
    private String name;
    private String state;
    
    // Constructors
    public DistrictDTO() {
    }
    
    public DistrictDTO(String name, String state) {
        this.name = name;
        this.state = state;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
}

