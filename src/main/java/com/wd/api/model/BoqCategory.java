package com.wd.api.model;

import jakarta.persistence.*;

/**
 * BOQ Category entity - supports hierarchical structure (Category -> Subcategory).
 * Categories are project-specific and can have parent-child relationships.
 */
@Entity
@Table(name = "boq_categories")
public class BoqCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BoqCategory parent;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (isActive == null) isActive = true;
        if (displayOrder == null) displayOrder = 0;
    }

    // ---- Helper methods ----

    @Transient
    public boolean isTopLevel() {
        return parent == null;
    }

    @Transient
    public boolean isSubcategory() {
        return parent != null;
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerProject getProject() { return project; }
    public void setProject(CustomerProject project) { this.project = project; }

    public BoqCategory getParent() { return parent; }
    public void setParent(BoqCategory parent) { this.parent = parent; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
