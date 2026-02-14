package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "subcontract_work_orders")
public class SubcontractWorkOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_number", nullable = false, unique = true)
    private String workOrderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boq_item_id")
    private BoqItem boqItem;

    @Column(name = "scope_description", columnDefinition = "TEXT", nullable = false)
    private String scopeDescription;

    @Column(name = "measurement_basis", nullable = false)
    @Enumerated(EnumType.STRING)
    private MeasurementBasis measurementBasis = MeasurementBasis.UNIT_RATE;

    @Column(name = "negotiated_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal negotiatedAmount;

    @Column(name = "unit")
    private String unit;

    @Column(name = "rate", precision = 15, scale = 2)
    private BigDecimal rate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "target_completion_date")
    private LocalDate targetCompletionDate;

    @Column(name = "actual_completion_date")
    private LocalDate actualCompletionDate;

    @Column(name = "payment_terms", columnDefinition = "TEXT")
    private String paymentTerms;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkOrderStatus status = WorkOrderStatus.DRAFT;

    @Column(name = "retention_percentage", precision = 5, scale = 2)
    private BigDecimal retentionPercentage = new BigDecimal("5.00"); // Default 5%

    @Column(name = "total_retention_accumulated", precision = 15, scale = 2)
    private BigDecimal totalRetentionAccumulated = BigDecimal.ZERO;

    public BigDecimal getRetentionPercentage() {
        return retentionPercentage;
    }

    public void setRetentionPercentage(BigDecimal retentionPercentage) {
        this.retentionPercentage = retentionPercentage;
    }

    public BigDecimal getTotalRetentionAccumulated() {
        return totalRetentionAccumulated;
    }

    public void setTotalRetentionAccumulated(BigDecimal totalRetentionAccumulated) {
        this.totalRetentionAccumulated = totalRetentionAccumulated;
    }

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
    }

    public enum MeasurementBasis {
        LUMPSUM, UNIT_RATE
    }

    public enum WorkOrderStatus {
        DRAFT, ISSUED, IN_PROGRESS, COMPLETED, TERMINATED
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public CustomerProject getProject() {
        return project;
    }

    public void setProject(CustomerProject project) {
        this.project = project;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public BoqItem getBoqItem() {
        return boqItem;
    }

    public void setBoqItem(BoqItem boqItem) {
        this.boqItem = boqItem;
    }

    public String getScopeDescription() {
        return scopeDescription;
    }

    public void setScopeDescription(String scopeDescription) {
        this.scopeDescription = scopeDescription;
    }

    public MeasurementBasis getMeasurementBasis() {
        return measurementBasis;
    }

    public void setMeasurementBasis(MeasurementBasis measurementBasis) {
        this.measurementBasis = measurementBasis;
    }

    public BigDecimal getNegotiatedAmount() {
        return negotiatedAmount;
    }

    public void setNegotiatedAmount(BigDecimal negotiatedAmount) {
        this.negotiatedAmount = negotiatedAmount;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getTargetCompletionDate() {
        return targetCompletionDate;
    }

    public void setTargetCompletionDate(LocalDate targetCompletionDate) {
        this.targetCompletionDate = targetCompletionDate;
    }

    public LocalDate getActualCompletionDate() {
        return actualCompletionDate;
    }

    public void setActualCompletionDate(LocalDate actualCompletionDate) {
        this.actualCompletionDate = actualCompletionDate;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public WorkOrderStatus getStatus() {
        return status;
    }

    public void setStatus(WorkOrderStatus status) {
        this.status = status;
    }
}
