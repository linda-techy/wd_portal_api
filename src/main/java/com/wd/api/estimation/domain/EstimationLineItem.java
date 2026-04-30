package com.wd.api.estimation.domain;

import com.wd.api.estimation.domain.enums.LineType;
import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "estimation_line_item")
public class EstimationLineItem extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "estimation_id", columnDefinition = "uuid", nullable = false)
    private UUID estimationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 30)
    private LineType lineType;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "source_ref_id", columnDefinition = "uuid")
    private UUID sourceRefId;

    @Column(name = "quantity", precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "unit_rate", precision = 10, scale = 2)
    private BigDecimal unitRate;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getEstimationId() { return estimationId; }
    public void setEstimationId(UUID estimationId) { this.estimationId = estimationId; }

    public LineType getLineType() { return lineType; }
    public void setLineType(LineType lineType) { this.lineType = lineType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getSourceRefId() { return sourceRefId; }
    public void setSourceRefId(UUID sourceRefId) { this.sourceRefId = sourceRefId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getUnitRate() { return unitRate; }
    public void setUnitRate(BigDecimal unitRate) { this.unitRate = unitRate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
