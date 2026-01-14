package com.wd.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generic DTO for representing enum values with display names and descriptions
 * Used for dropdown/select lists in the frontend
 */
public class EnumValueDTO {
    
    private String value;
    
    @JsonProperty("display_name")
    private String displayName;
    
    private String description;
    
    private Integer order;
    
    // Constructors
    public EnumValueDTO() {
    }
    
    public EnumValueDTO(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
    
    public EnumValueDTO(String value, String displayName, String description) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
    }
    
    public EnumValueDTO(String value, String displayName, String description, Integer order) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
        this.order = order;
    }
    
    // Getters and Setters
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getOrder() {
        return order;
    }
    
    public void setOrder(Integer order) {
        this.order = order;
    }
}

