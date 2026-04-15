package com.wd.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProjectMemberRequest {

    @JsonProperty("customer_user_id")
    private Long customerUserId;

    @JsonProperty("role_in_project")
    private String roleInProject;

    public ProjectMemberRequest() {
    }

    public Long getCustomerUserId() {
        return customerUserId;
    }

    public void setCustomerUserId(Long customerUserId) {
        this.customerUserId = customerUserId;
    }

    public String getRoleInProject() {
        return roleInProject;
    }

    public void setRoleInProject(String roleInProject) {
        this.roleInProject = roleInProject;
    }
}
