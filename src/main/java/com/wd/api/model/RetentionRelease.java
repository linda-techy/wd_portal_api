package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "retention_releases")
public class RetentionRelease extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private SubcontractWorkOrder workOrder;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(name = "amount_released", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountReleased;

    @Column(name = "notes")
    private String notes;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReleaseStatus status = ReleaseStatus.PENDING;

    @Column(name = "approved_by_id")
    private Long approvedById;

    public enum ReleaseStatus {
        PENDING, APPROVED, PAID
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SubcontractWorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(SubcontractWorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public BigDecimal getAmountReleased() {
        return amountReleased;
    }

    public void setAmountReleased(BigDecimal amountReleased) {
        this.amountReleased = amountReleased;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public ReleaseStatus getStatus() {
        return status;
    }

    public void setStatus(ReleaseStatus status) {
        this.status = status;
    }

    public Long getApprovedById() {
        return approvedById;
    }

    public void setApprovedById(Long approvedById) {
        this.approvedById = approvedById;
    }
}
