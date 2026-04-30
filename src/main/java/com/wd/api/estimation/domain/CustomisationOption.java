package com.wd.api.estimation.domain;

import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "estimation_customisation_option")
@SQLDelete(sql = "UPDATE estimation_customisation_option SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
public class CustomisationOption extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "category_id", columnDefinition = "uuid", nullable = false)
    private UUID categoryId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal rate;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
