package com.wd.api.estimation.domain;

import com.wd.api.estimation.domain.enums.SiteFeeMode;
import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "estimation_site_fee")
@SQLDelete(sql = "UPDATE estimation_site_fee SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
public class SiteFee extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private SiteFeeMode mode;

    @Column(name = "lump_amount", precision = 14, scale = 2)
    private BigDecimal lumpAmount;

    @Column(name = "per_sqft_rate", precision = 10, scale = 2)
    private BigDecimal perSqftRate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public SiteFeeMode getMode() { return mode; }
    public void setMode(SiteFeeMode mode) { this.mode = mode; }

    public BigDecimal getLumpAmount() { return lumpAmount; }
    public void setLumpAmount(BigDecimal lumpAmount) { this.lumpAmount = lumpAmount; }

    public BigDecimal getPerSqftRate() { return perSqftRate; }
    public void setPerSqftRate(BigDecimal perSqftRate) { this.perSqftRate = perSqftRate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
