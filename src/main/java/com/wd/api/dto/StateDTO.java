package com.wd.api.dto;

import java.util.List;

/**
 * DTO for Indian State master data
 */
public class StateDTO {
    
    private String name;
    private String code;
    private List<String> districts;
    
    // Constructors
    public StateDTO() {
    }
    
    public StateDTO(String name, String code) {
        this.name = name;
        this.code = code;
    }
    
    public StateDTO(String name, String code, List<String> districts) {
        this.name = name;
        this.code = code;
        this.districts = districts;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public List<String> getDistricts() {
        return districts;
    }
    
    public void setDistricts(List<String> districts) {
        this.districts = districts;
    }
}

