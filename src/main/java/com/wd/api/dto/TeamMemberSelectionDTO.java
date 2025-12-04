package com.wd.api.dto;

public class TeamMemberSelectionDTO {
    private Long id;
    private String type; // "PORTAL" or "CUSTOMER"

    public TeamMemberSelectionDTO() {
    }

    public TeamMemberSelectionDTO(Long id, String type) {
        this.id = id;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
