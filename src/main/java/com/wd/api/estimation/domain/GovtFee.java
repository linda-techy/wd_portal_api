package com.wd.api.estimation.domain;

import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "estimation_govt_fee")
@SQLDelete(sql = "UPDATE estimation_govt_fee SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
public class GovtFee extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "lump_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal lumpAmount;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getLumpAmount() { return lumpAmount; }
    public void setLumpAmount(BigDecimal lumpAmount) { this.lumpAmount = lumpAmount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
