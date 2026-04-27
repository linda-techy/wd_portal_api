package com.wd.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * "Options considered" card under a DPC scope template.
 *
 * Each card represents one alternative the customer could have chosen for a
 * scope (e.g. for FOUNDATION: RANDOM_RUBBLE, RAFT, COLUMN_FOOTING, PILE).
 * Uniqueness of (scope_template_id, code) is enforced at the DB level.
 */
@SQLDelete(sql = "UPDATE dpc_scope_option SET deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "dpc_scope_option")
public class DpcScopeOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scope_template_id", nullable = false)
    private DpcScopeTemplate scopeTemplate;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (displayOrder == null) displayOrder = 0;
        if (isActive == null) isActive = true;
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DpcScopeTemplate getScopeTemplate() { return scopeTemplate; }
    public void setScopeTemplate(DpcScopeTemplate scopeTemplate) { this.scopeTemplate = scopeTemplate; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
