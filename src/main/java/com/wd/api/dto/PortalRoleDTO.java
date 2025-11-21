package com.wd.api.dto;

import com.wd.api.model.PortalRole;

public class PortalRoleDTO {
    private Long id;
    private String name;
    private String description;
    private String code;
    
    public PortalRoleDTO() {}
    
    public PortalRoleDTO(PortalRole role) {
        this.id = role.getId();
        this.name = role.getName();
        this.description = role.getDescription();
        this.code = role.getCode();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
}

