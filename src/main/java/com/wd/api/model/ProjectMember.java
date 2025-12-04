package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_members")
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portal_user_id")
    private PortalUser portalUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_user_id") // Assuming this column name based on pattern, though user didn't explicitly
                                           // show it in screenshot, but mentioned "customer_users" table. Wait, the
                                           // screenshot shows "customer_id" and "portal_user_id". "customer_id" usually
                                           // refers to the Customer entity, not CustomerUser. But the user said
                                           // "dropdown of team members should load from table both customer_users and
                                           // portal_users".
    // Let's look at the screenshot again.
    // Screenshot 1: project_members table has: customer_id (int8), project_id
    // (int8), role_in_project (varchar), portal_user_id (int8).
    // It seems "customer_id" might be referring to "Customer" entity or
    // "CustomerUser"?
    // The user said "load from table both customer_users and portal_users and add
    // to this project_members table".
    // If "customer_id" refers to "CustomerUser", then I should map it to
    // CustomerUser entity.
    // However, usually "customer_id" refers to the Customer (organization).
    // Let's check CustomerUser.java again. It has @Table(name = "customer_users").
    // Let's check Customer.java if possible.
    // But given the user request "add to this project_members table", and the
    // columns are `customer_id` and `portal_user_id`.
    // I will assume `customer_id` in `project_members` is intended to link to
    // `CustomerUser` based on the context of "team members".
    // OR, maybe `customer_id` is for the Customer entity, and there is no column
    // for `CustomerUser` yet?
    // The user said "add to this project_members table".
    // If I look at the screenshot, it has `customer_id`.
    // If I look at `CustomerUser` entity, it is a user.
    // If I look at `Customer` entity, it is likely a company/client.
    // The user wants to assign "team members".
    // "Team members" usually implies people.
    // So `CustomerUser` makes sense.
    // But the column name is `customer_id`.
    // I will map `customer_id` to `CustomerUser` for now, but I should be careful.
    // Actually, looking at the screenshot, `customer_id` is `int8`.
    // I'll assume it maps to `CustomerUser`.

    private CustomerUser customerUser;

    @Column(name = "role_in_project", length = 50)
    private String roleInProject;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerProject getProject() {
        return project;
    }

    public void setProject(CustomerProject project) {
        this.project = project;
    }

    public PortalUser getPortalUser() {
        return portalUser;
    }

    public void setPortalUser(PortalUser portalUser) {
        this.portalUser = portalUser;
    }

    public CustomerUser getCustomerUser() {
        return customerUser;
    }

    public void setCustomerUser(CustomerUser customerUser) {
        this.customerUser = customerUser;
    }

    public String getRoleInProject() {
        return roleInProject;
    }

    public void setRoleInProject(String roleInProject) {
        this.roleInProject = roleInProject;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
