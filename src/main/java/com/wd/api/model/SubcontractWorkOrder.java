package com.wd.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Subcontract Work Order Entity
 * Tracks piece-rate and lump-sum subcontractor agreements
 * 
 * Business Context:
 * - In Indian construction, 60-70% of work is subcontracted
 * - Unit-rate: e.g., ₹450/sqft for plastering
 * - Lump-sum: e.g., ₹3.5L for entire electrical work
 */
@Entity
@Table(name = "subcontract_work_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubcontractWorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_number", unique = true, nullable = false, length = 50)
    private String workOrderNumber; // WAL/SC/YY/NNN

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor; // vendor_type must be 'LABOUR'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boq_item_id")
    private BoqItem boqItem;

    // Scope and Terms
    @Column(name = "scope_description", columnDefinition = "TEXT", nullable = false)
    private String scopeDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_basis", nullable = false, length = 20)
    private MeasurementBasis measurementBasis;

    @Column(name = "negotiated_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal negotiatedAmount;

    @Column(name = "unit", length = 50)
    private String unit; // For unit-rate contracts

    @Column(name = "rate", precision = 15, scale = 2)
    private BigDecimal rate; // For unit-rate contracts

    // Timeline
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "target_completion_date")
    private LocalDate targetCompletionDate;

    @Column(name = "actual_completion_date")
    private LocalDate actualCompletionDate;

    // Payment Terms
    @Column(name = "payment_terms", columnDefinition = "TEXT")
    private String paymentTerms; // Human-readable description

    @Column(name = "advance_percentage", precision = 5, scale = 2)
    private BigDecimal advancePercentage;

    @Column(name = "advance_paid", precision = 15, scale = 2)
    private BigDecimal advancePaid;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkOrderStatus status;

    // Tracking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private PortalUser createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Notes
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "termination_reason", columnDefinition = "TEXT")
    private String terminationReason;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = WorkOrderStatus.DRAFT;
        }
        if (advancePaid == null) {
            advancePaid = BigDecimal.ZERO;
        }
        if (advancePercentage == null) {
            advancePercentage = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Enums
    public enum MeasurementBasis {
        LUMPSUM, // Fixed price for entire scope
        UNIT_RATE // Price per unit of measurement
    }

    public enum WorkOrderStatus {
        DRAFT, // Being prepared
        ISSUED, // Sent to subcontractor
        IN_PROGRESS, // Work started
        COMPLETED, // Work finished
        TERMINATED // Contract terminated
    }

    // Helper methods
    public boolean isUnitRateContract() {
        return measurementBasis == MeasurementBasis.UNIT_RATE;
    }

    public boolean isLumpsumContract() {
        return measurementBasis == MeasurementBasis.LUMPSUM;
    }

    public boolean isActive() {
        return status == WorkOrderStatus.ISSUED || status == WorkOrderStatus.IN_PROGRESS;
    }

    public boolean canBeIssued() {
        return status == WorkOrderStatus.DRAFT;
    }

    public boolean canRecordPayment() {
        return status != WorkOrderStatus.DRAFT && status != WorkOrderStatus.TERMINATED;
    }
}
