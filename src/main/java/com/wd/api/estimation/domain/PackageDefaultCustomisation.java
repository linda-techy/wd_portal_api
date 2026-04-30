package com.wd.api.estimation.domain;

import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "estimation_package_default_customisation",
       uniqueConstraints = @UniqueConstraint(columnNames = {"package_id", "category_id"}))
@SQLDelete(sql = "UPDATE estimation_package_default_customisation SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
public class PackageDefaultCustomisation extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "package_id", columnDefinition = "uuid", nullable = false)
    private UUID packageId;

    @Column(name = "category_id", columnDefinition = "uuid", nullable = false)
    private UUID categoryId;

    @Column(name = "option_id", columnDefinition = "uuid", nullable = false)
    private UUID optionId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPackageId() { return packageId; }
    public void setPackageId(UUID packageId) { this.packageId = packageId; }

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public UUID getOptionId() { return optionId; }
    public void setOptionId(UUID optionId) { this.optionId = optionId; }
}
