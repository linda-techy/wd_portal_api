package com.wd.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wd.api.model.CustomerUser;
import java.time.LocalDateTime;

public class CustomerResponse {
    private Long id;
    private String email;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    private Boolean enabled;

    @JsonProperty("role_id")
    private Long roleId;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public CustomerResponse() {
    }

    public CustomerResponse(CustomerUser customerUser) {
        if (customerUser == null) {
            throw new IllegalArgumentException("CustomerUser cannot be null");
        }
        this.id = customerUser.getId();
        this.email = customerUser.getEmail() != null ? customerUser.getEmail() : "";
        this.firstName = customerUser.getFirstName() != null ? customerUser.getFirstName() : "";
        this.lastName = customerUser.getLastName() != null ? customerUser.getLastName() : "";
        this.enabled = customerUser.getEnabled() != null ? customerUser.getEnabled() : true;
        this.roleId = customerUser.getRole() != null ? customerUser.getRole().getId() : null;
        this.createdAt = customerUser.getCreatedAt();
        this.updatedAt = customerUser.getUpdatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @JsonProperty("project_count")
    private int projectCount;

    public int getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(int projectCount) {
        this.projectCount = projectCount;
    }
}
