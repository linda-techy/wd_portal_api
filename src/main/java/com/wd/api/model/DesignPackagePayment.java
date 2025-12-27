package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.wd.api.model.CustomerProject;

@Entity
@Table(name = "design_package_payments")
public class DesignPackagePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private CustomerProject project;

    @Column(name = "package_name", nullable = false, length = 50)
    private String packageName;

    @Column(name = "rate_per_sqft", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerSqft;

    @Column(name = "total_sqft", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalSqft;

    @Column(name = "base_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "gst_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercentage = new BigDecimal("18.00");

    @Column(name = "gst_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal gstAmount;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_type", nullable = false, length = 20)
    private String paymentType; // 'FULL' or 'INSTALLMENT'

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // 'PENDING', 'PARTIAL', 'PAID'

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by_id")
    private Long createdById;

    // Retention Money Tracking
    @Column(name = "retention_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal retentionPercentage = new BigDecimal("10.00");

    @Column(name = "retention_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal retentionAmount = BigDecimal.ZERO;

    @Column(name = "retention_released_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal retentionReleasedAmount = BigDecimal.ZERO;

    @Column(name = "defect_liability_end_date")
    private java.time.LocalDate defectLiabilityEndDate;

    @Column(name = "retention_status", length = 20, nullable = false)
    private String retentionStatus = "ACTIVE"; // 'ACTIVE', 'PARTIALLY_RELEASED', 'RELEASED'

    @OneToMany(mappedBy = "designPayment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentSchedule> schedules = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateRetention();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateRetention();
    }

    private void calculateRetention() {
        if (totalAmount != null && retentionPercentage != null) {
            this.retentionAmount = totalAmount.multiply(retentionPercentage)
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return project != null ? project.getId() : null;
    }

    public CustomerProject getProject() {
        return project;
    }

    public void setProject(CustomerProject project) {
        this.project = project;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public BigDecimal getRatePerSqft() {
        return ratePerSqft;
    }

    public void setRatePerSqft(BigDecimal ratePerSqft) {
        this.ratePerSqft = ratePerSqft;
    }

    public BigDecimal getTotalSqft() {
        return totalSqft;
    }

    public void setTotalSqft(BigDecimal totalSqft) {
        this.totalSqft = totalSqft;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public BigDecimal getGstPercentage() {
        return gstPercentage;
    }

    public void setGstPercentage(BigDecimal gstPercentage) {
        this.gstPercentage = gstPercentage;
    }

    public BigDecimal getGstAmount() {
        return gstAmount;
    }

    public void setGstAmount(BigDecimal gstAmount) {
        this.gstAmount = gstAmount;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public List<PaymentSchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<PaymentSchedule> schedules) {
        this.schedules = schedules;
    }

    public void addSchedule(PaymentSchedule schedule) {
        schedules.add(schedule);
        schedule.setDesignPayment(this);
    }

    public BigDecimal getRetentionPercentage() {
        return retentionPercentage;
    }

    public void setRetentionPercentage(BigDecimal retentionPercentage) {
        this.retentionPercentage = retentionPercentage;
    }

    public BigDecimal getRetentionAmount() {
        return retentionAmount;
    }

    public void setRetentionAmount(BigDecimal retentionAmount) {
        this.retentionAmount = retentionAmount;
    }

    public BigDecimal getRetentionReleasedAmount() {
        return retentionReleasedAmount;
    }

    public void setRetentionReleasedAmount(BigDecimal retentionReleasedAmount) {
        this.retentionReleasedAmount = retentionReleasedAmount;
    }

    public java.time.LocalDate getDefectLiabilityEndDate() {
        return defectLiabilityEndDate;
    }

    public void setDefectLiabilityEndDate(java.time.LocalDate defectLiabilityEndDate) {
        this.defectLiabilityEndDate = defectLiabilityEndDate;
    }

    public String getRetentionStatus() {
        return retentionStatus;
    }

    public void setRetentionStatus(String retentionStatus) {
        this.retentionStatus = retentionStatus;
    }
}
