package com.wd.api.estimation.domain;

import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "estimation_market_index_snapshot")
public class MarketIndexSnapshot extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "steel_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal steelRate;

    @Column(name = "cement_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal cementRate;

    @Column(name = "sand_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal sandRate;

    @Column(name = "aggregate_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal aggregateRate;

    @Column(name = "tiles_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal tilesRate;

    @Column(name = "electrical_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal electricalRate;

    @Column(name = "paints_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal paintsRate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weights_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> weightsJson;

    @Column(name = "composite_index", nullable = false, precision = 6, scale = 4)
    private BigDecimal compositeIndex;

    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }

    public BigDecimal getSteelRate() { return steelRate; }
    public void setSteelRate(BigDecimal steelRate) { this.steelRate = steelRate; }

    public BigDecimal getCementRate() { return cementRate; }
    public void setCementRate(BigDecimal cementRate) { this.cementRate = cementRate; }

    public BigDecimal getSandRate() { return sandRate; }
    public void setSandRate(BigDecimal sandRate) { this.sandRate = sandRate; }

    public BigDecimal getAggregateRate() { return aggregateRate; }
    public void setAggregateRate(BigDecimal aggregateRate) { this.aggregateRate = aggregateRate; }

    public BigDecimal getTilesRate() { return tilesRate; }
    public void setTilesRate(BigDecimal tilesRate) { this.tilesRate = tilesRate; }

    public BigDecimal getElectricalRate() { return electricalRate; }
    public void setElectricalRate(BigDecimal electricalRate) { this.electricalRate = electricalRate; }

    public BigDecimal getPaintsRate() { return paintsRate; }
    public void setPaintsRate(BigDecimal paintsRate) { this.paintsRate = paintsRate; }

    public Map<String, Object> getWeightsJson() { return weightsJson; }
    public void setWeightsJson(Map<String, Object> weightsJson) { this.weightsJson = weightsJson; }

    public BigDecimal getCompositeIndex() { return compositeIndex; }
    public void setCompositeIndex(BigDecimal compositeIndex) { this.compositeIndex = compositeIndex; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
