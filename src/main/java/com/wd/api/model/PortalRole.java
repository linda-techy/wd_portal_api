package com.wd.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "portal_roles")
public class PortalRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String code;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "portal_role_permissions", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private java.util.Set<Permission> permissions;

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

    public java.util.Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(java.util.Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
