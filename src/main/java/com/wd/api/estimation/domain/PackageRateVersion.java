package com.wd.api.estimation.domain;

import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "estimation_package_rate_version")
public class PackageRateVersion extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "package_id", columnDefinition = "uuid", nullable = false)
    private UUID packageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false, length = 20)
    private ProjectType projectType;

    @Column(name = "material_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal materialRate;

    @Column(name = "labour_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal labourRate;

    @Column(name = "overhead_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal overheadRate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPackageId() { return packageId; }
    public void setPackageId(UUID packageId) { this.packageId = packageId; }

    public ProjectType getProjectType() { return projectType; }
    public void setProjectType(ProjectType projectType) { this.projectType = projectType; }

    public BigDecimal getMaterialRate() { return materialRate; }
    public void setMaterialRate(BigDecimal materialRate) { this.materialRate = materialRate; }

    public BigDecimal getLabourRate() { return labourRate; }
    public void setLabourRate(BigDecimal labourRate) { this.labourRate = labourRate; }

    public BigDecimal getOverheadRate() { return overheadRate; }
    public void setOverheadRate(BigDecimal overheadRate) { this.overheadRate = overheadRate; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
}
