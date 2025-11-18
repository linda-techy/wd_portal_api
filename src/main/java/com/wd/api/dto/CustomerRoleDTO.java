package com.wd.api.dto;

import com.wd.api.model.CustomerRole;

public class CustomerRoleDTO {
    private Long id;
    private String name;
    private String description;
    
    // Constructors
    public CustomerRoleDTO() {}
    
    public CustomerRoleDTO(CustomerRole role) {
        this.id = role.getId();
        this.name = role.getName();
        this.description = role.getDescription();
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
}

