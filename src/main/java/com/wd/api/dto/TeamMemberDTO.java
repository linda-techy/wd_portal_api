package com.wd.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TeamMemberDTO {

    private Long id;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("full_name")
    private String fullName;

    private String email;

    private String type; // "PORTAL" or "CUSTOMER"

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("role_id")
    private Long roleId;

    @JsonProperty("role_name")
    private String roleName;

    private String designation;

    private String department;

    public TeamMemberDTO() {
    }

    /** Backward-compat constructor (no status / role). */
    public TeamMemberDTO(Long id, String firstName, String lastName, String email, String type) {
        this(id, firstName, lastName, email, type, true, null, null, null, null);
    }

    public TeamMemberDTO(Long id, String firstName, String lastName, String email, String type,
                         Boolean isActive, Long roleId, String roleName,
                         String designation, String department) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.fullName = (firstName == null ? "" : firstName) + " "
                + (lastName == null ? "" : lastName);
        this.type = type;
        this.isActive = isActive;
        this.roleId = roleId;
        this.roleName = roleName;
        this.designation = designation;
        this.department = department;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; updateFullName(); }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; updateFullName(); }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    private void updateFullName() {
        this.fullName = (firstName == null ? "" : firstName) + " "
                + (lastName == null ? "" : lastName);
    }
}
