package com.wd.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

public class CustomerUpdateRequest {
    private String email;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    private String password; // Optional - only if changing password
    private Boolean enabled;
    
    private Long roleId;
    
    // Custom setter to handle both Integer and Long from JSON
    @JsonProperty("role_id")
    @JsonSetter("role_id")
    public void setRoleId(Number roleId) {
        if (roleId != null) {
            this.roleId = roleId.longValue();
        } else {
            this.roleId = null;
        }
    }

    // Constructors
    public CustomerUpdateRequest() {}

    public CustomerUpdateRequest(String email, String firstName, String lastName, String password, Boolean enabled, Long roleId) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.enabled = enabled;
        this.roleId = roleId;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getRoleId() {
        return roleId;
    }
}

