package com.wd.api.estimation.domain;

import com.wd.api.estimation.domain.enums.EstimationStatus;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "estimation")
@SQLDelete(sql = "UPDATE estimation SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
public class Estimation extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "estimation_no", nullable = false, unique = true, length = 30)
    private String estimationNo;

    @Column(name = "lead_id", nullable = false)
    private Long leadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false, length = 20)
    private ProjectType projectType;

    @Column(name = "package_id", columnDefinition = "uuid")
    private UUID packageId;

    @Column(name = "rate_version_id", columnDefinition = "uuid")
    private UUID rateVersionId;

    @Column(name = "market_index_id", columnDefinition = "uuid")
    private UUID marketIndexId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dimensions_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> dimensionsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EstimationStatus status = EstimationStatus.DRAFT;

    @Column(name = "subtotal", precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", precision = 14, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "gst_amount", precision = 14, scale = 2)
    private BigDecimal gstAmount;

    @Column(name = "grand_total", precision = 14, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "public_view_token", nullable = false, columnDefinition = "uuid")
    private UUID publicViewToken = UUID.randomUUID();

    @Column(name = "parent_estimation_id", columnDefinition = "uuid")
    private UUID parentEstimationId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEstimationNo() { return estimationNo; }
    public void setEstimationNo(String estimationNo) { this.estimationNo = estimationNo; }

    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }

    public ProjectType getProjectType() { return projectType; }
    public void setProjectType(ProjectType projectType) { this.projectType = projectType; }

    public UUID getPackageId() { return packageId; }
    public void setPackageId(UUID packageId) { this.packageId = packageId; }

    public UUID getRateVersionId() { return rateVersionId; }
    public void setRateVersionId(UUID rateVersionId) { this.rateVersionId = rateVersionId; }

    public UUID getMarketIndexId() { return marketIndexId; }
    public void setMarketIndexId(UUID marketIndexId) { this.marketIndexId = marketIndexId; }

    public Map<String, Object> getDimensionsJson() { return dimensionsJson; }
    public void setDimensionsJson(Map<String, Object> dimensionsJson) { this.dimensionsJson = dimensionsJson; }

    public EstimationStatus getStatus() { return status; }
    public void setStatus(EstimationStatus status) { this.status = status; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public UUID getPublicViewToken() { return publicViewToken; }
    public void setPublicViewToken(UUID publicViewToken) { this.publicViewToken = publicViewToken; }

    public UUID getParentEstimationId() { return parentEstimationId; }
    public void setParentEstimationId(UUID parentEstimationId) { this.parentEstimationId = parentEstimationId; }
}
