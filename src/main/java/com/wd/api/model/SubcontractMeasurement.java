package com.wd.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Subcontract Measurement Entity
 * Progress measurement for unit-rate subcontracts
 * 
 * Business Context:
 * - Similar to Measurement Book (MB) but for subcontractors
 * - Site engineer measures work done (e.g., 100 sqft plastered)
 * - Measurement needs approval before payment
 * - Multiple measurements create running bills (RA Bill 1, 2, 3...)
 */
@Entity
@Table(name = "subcontract_measurements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubcontractMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private SubcontractWorkOrder workOrder;

    @Column(name = "measurement_date", nullable = false)
    private LocalDate measurementDate;

    // Measurement Details
    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "quantity", precision = 15, scale = 2, nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    @Column(name = "rate", precision = 15, scale = 2, nullable = false)
    private BigDecimal rate;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount; // quantity * rate

    // Running Bill Number
    @Column(name = "bill_number", length = 50)
    private String billNumber; // e.g., "RA Bill 1", "RA Bill 2"

    // Approval Workflow
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MeasurementStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private PortalUser approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Tracking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measured_by_id")
    private PortalUser measuredBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = MeasurementStatus.PENDING;
        }
        // Auto-calculate amount
        if (quantity != null && rate != null) {
            amount = quantity.multiply(rate);
        }
    }

    // Enums
    public enum MeasurementStatus {
        PENDING, // Awaiting approval
        APPROVED, // Approved for payment
        REJECTED // Rejected
    }

    // Helper methods
    public boolean isPending() {
        return status == MeasurementStatus.PENDING;
    }

    public boolean isApproved() {
        return status == MeasurementStatus.APPROVED;
    }

    public void approve(PortalUser approver) {
        this.status = MeasurementStatus.APPROVED;
        this.approvedBy = approver;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(PortalUser rejector, String reason) {
        this.status = MeasurementStatus.REJECTED;
        this.approvedBy = rejector;
        this.approvedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    public void calculateAmount() {
        if (quantity != null && rate != null) {
            this.amount = quantity.multiply(rate);
        }
    }
}
