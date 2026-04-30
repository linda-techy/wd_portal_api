package com.wd.api.estimation.domain;

import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "estimation_package")
@SQLDelete(sql = "UPDATE estimation_package SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
public class EstimationPackage extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "internal_name", nullable = false, unique = true, length = 50)
    private PackageInternalName internalName;

    @Column(name = "marketing_name", nullable = false, length = 100)
    private String marketingName;

    @Column(name = "tagline", length = 255)
    private String tagline;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PackageInternalName getInternalName() { return internalName; }
    public void setInternalName(PackageInternalName internalName) { this.internalName = internalName; }

    public String getMarketingName() { return marketingName; }
    public void setMarketingName(String marketingName) { this.marketingName = marketingName; }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
