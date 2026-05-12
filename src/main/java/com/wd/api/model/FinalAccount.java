package com.wd.api.model;

import com.wd.api.model.enums.FinalAccountStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Final Account — one per project. Summarises the complete financial reconciliation
 * at project close: base contract + VOs + deductions + payments + retention.
 *
 * Status lifecycle: DRAFT → SUBMITTED → DISPUTED → AGREED → CLOSED
 *
 * Computed fields (not stored):
 *   net_revised_contract_value = baseContractValue + totalAdditions - totalAcceptedDeductions
 *   balance_payable            = netRevisedContractValue - totalReceivedToDate
 *
 * UNIQUE constraint on project_id enforces one final account per project.
 */
@Entity
@Table(name = "final_account")
public class FinalAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private CustomerProject project;

    // ---- Stored financial totals ----

    @Column(name = "base_contract_value", precision = 18, scale = 6, nullable = false)
    private BigDecimal baseContractValue = BigDecimal.ZERO;

    @Column(name = "total_additions", precision = 18, scale = 6, nullable = false)
    private BigDecimal totalAdditions = BigDecimal.ZERO;

    @Column(name = "total_accepted_deductions", precision = 18, scale = 6, nullable = false)
    private BigDecimal totalAcceptedDeductions = BigDecimal.ZERO;

    @Column(name = "total_received_to_date", precision = 18, scale = 6, nullable = false)
    private BigDecimal totalReceivedToDate = BigDecimal.ZERO;

    @Column(name = "total_retention_held", precision = 18, scale = 6, nullable = false)
    private BigDecimal totalRetentionHeld = BigDecimal.ZERO;

    // ---- Status ----

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FinalAccountStatus status = FinalAccountStatus.DRAFT;

    // ---- DLP / Retention release ----

    @Column(name = "dlp_start_date")
    private LocalDate dlpStartDate;

    @Column(name = "dlp_end_date")
    private LocalDate dlpEndDate;

    @Column(name = "retention_released", nullable = false)
    private boolean retentionReleased = false;

    @Column(name = "retention_release_date")
    private LocalDate retentionReleaseDate;

    // ---- Signatories ----

    @Column(name = "prepared_by", length = 100)
    private String preparedBy;

    @Column(name = "agreed_by", length = 100)
    private String agreedBy;

    // ---- Concurrency (G-19) ----

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    // ---- Audit ----

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = FinalAccountStatus.DRAFT;
        if (baseContractValue == null)       baseContractValue       = BigDecimal.ZERO;
        if (totalAdditions == null)          totalAdditions          = BigDecimal.ZERO;
        if (totalAcceptedDeductions == null) totalAcceptedDeductions = BigDecimal.ZERO;
        if (totalReceivedToDate == null)     totalReceivedToDate     = BigDecimal.ZERO;
        if (totalRetentionHeld == null)      totalRetentionHeld      = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ---- Computed helpers (not persisted) ----

    @Transient
    public BigDecimal getNetRevisedContractValue() {
        return baseContractValue
                .add(totalAdditions)
                .subtract(totalAcceptedDeductions);
    }

    @Transient
    public BigDecimal getBalancePayable() {
        return getNetRevisedContractValue().subtract(totalReceivedToDate);
    }

    @Transient
    public boolean isDraft()     { return FinalAccountStatus.DRAFT     == status; }

    @Transient
    public boolean isAgreed()    { return FinalAccountStatus.AGREED    == status; }

    @Transient
    public boolean isClosed()    { return FinalAccountStatus.CLOSED    == status; }

    @Transient
    public boolean isDisputed()  { return FinalAccountStatus.DISPUTED  == status; }

    // ---- Getters and Setters ----

    public Long getId() { return id; }

    public CustomerProject getProject() { return project; }
    public void setProject(CustomerProject project) { this.project = project; }

    public BigDecimal getBaseContractValue() { return baseContractValue; }
    public void setBaseContractValue(BigDecimal baseContractValue) { this.baseContractValue = baseContractValue; }

    public BigDecimal getTotalAdditions() { return totalAdditions; }
    public void setTotalAdditions(BigDecimal totalAdditions) { this.totalAdditions = totalAdditions; }

    public BigDecimal getTotalAcceptedDeductions() { return totalAcceptedDeductions; }
    public void setTotalAcceptedDeductions(BigDecimal totalAcceptedDeductions) { this.totalAcceptedDeductions = totalAcceptedDeductions; }

    public BigDecimal getTotalReceivedToDate() { return totalReceivedToDate; }
    public void setTotalReceivedToDate(BigDecimal totalReceivedToDate) { this.totalReceivedToDate = totalReceivedToDate; }

    public BigDecimal getTotalRetentionHeld() { return totalRetentionHeld; }
    public void setTotalRetentionHeld(BigDecimal totalRetentionHeld) { this.totalRetentionHeld = totalRetentionHeld; }

    public FinalAccountStatus getStatus() { return status; }
    public void setStatus(FinalAccountStatus status) { this.status = status; }

    public LocalDate getDlpStartDate() { return dlpStartDate; }
    public void setDlpStartDate(LocalDate dlpStartDate) { this.dlpStartDate = dlpStartDate; }

    public LocalDate getDlpEndDate() { return dlpEndDate; }
    public void setDlpEndDate(LocalDate dlpEndDate) { this.dlpEndDate = dlpEndDate; }

    public boolean isRetentionReleased() { return retentionReleased; }
    public void setRetentionReleased(boolean retentionReleased) { this.retentionReleased = retentionReleased; }

    public LocalDate getRetentionReleaseDate() { return retentionReleaseDate; }
    public void setRetentionReleaseDate(LocalDate retentionReleaseDate) { this.retentionReleaseDate = retentionReleaseDate; }

    public String getPreparedBy() { return preparedBy; }
    public void setPreparedBy(String preparedBy) { this.preparedBy = preparedBy; }

    public String getAgreedBy() { return agreedBy; }
    public void setAgreedBy(String agreedBy) { this.agreedBy = agreedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
