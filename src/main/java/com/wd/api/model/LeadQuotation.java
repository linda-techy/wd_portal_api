package com.wd.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a quotation/proposal sent to a lead.
 * Supports versioning, status tracking, line items, and soft-delete (V74).
 *
 * <p>Soft-delete is wired with {@code @SQLDelete} so calling
 * {@code repo.delete(quotation)} performs a tombstone UPDATE rather than a
 * destructive DELETE. {@code @Where} auto-filters tombstoned rows from every
 * normal query, so existing code continues to "see only live" without
 * change. Restore is via the explicit native query in
 * {@code LeadQuotationRepository.restoreById}.
 */
@Entity
@Table(name = "lead_quotations")
@SQLDelete(sql = "UPDATE lead_quotations SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class LeadQuotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_id", nullable = false)
    private Long leadId;

    @Column(name = "quotation_number", nullable = false, unique = true, length = 50)
    private String quotationNumber;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount;

    /**
     * Optional tax rate as a percentage (e.g. {@code 18.00} for India's
     * standard 18% GST). When set, {@code taxAmount} is computed in the
     * service layer as {@code (subtotal - discount) × rate / 100} so the
     * tax base is always the discounted amount — matching Indian invoicing
     * convention. When {@code null}, {@code taxAmount} is treated as a
     * staff-entered manual override and used as-is.
     *
     * <p>New quotations default to 18% (Indian standard residential GST).
     * Loading an existing row from the DB preserves whatever value (including
     * NULL) is on disk, so legacy rows continue to behave as before.
     */
    @Column(name = "tax_rate_percent", precision = 5, scale = 2)
    private BigDecimal taxRatePercent = new BigDecimal("18.00");

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "validity_days")
    private Integer validityDays = 30;

    @Column(nullable = false, length = 50)
    private String status = "DRAFT"; // DRAFT, SENT, VIEWED, ACCEPTED, REJECTED, EXPIRED

    /**
     * How the headline figure is derived:
     *
     * <ul>
     *   <li>{@code LINE_ITEM} — legacy: subtotal = sum(item.totalPrice).</li>
     *   <li>{@code SQFT_RATE} — Walldot's standard customer quotation:
     *       subtotal = {@code lead.projectSqftArea × ratePerSqft}; items are
     *       scope specifications (description + brand + max-cost ceiling)
     *       with no per-line prices.</li>
     * </ul>
     *
     * <p>The entity default stays {@code LINE_ITEM} so a bare
     * {@code new LeadQuotation()} preserves the old math (matters for tests
     * and any service-layer instantiation). The Flutter add screen sets
     * {@code SQFT_RATE} explicitly for new customer-facing quotations —
     * that's where the customer-facing default lives.
     */
    @Column(name = "pricing_mode", nullable = false, length = 20)
    private String pricingMode = "LINE_ITEM";

    /**
     * Headline per-sqft rate for {@link #pricingMode} = {@code SQFT_RATE}.
     * Multiplied by {@code lead.projectSqftArea} to produce the subtotal.
     * {@code null} for {@code LINE_ITEM} mode.
     */
    @Column(name = "rate_per_sqft", precision = 12, scale = 2)
    private BigDecimal ratePerSqft;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Soft-delete tombstone. NULL = live row. Set automatically by the
     * {@link SQLDelete} statement when {@code repo.delete} is invoked;
     * cleared by {@code LeadQuotationRepository.restoreById} via a native
     * query that bypasses the {@code @SQLRestriction} filter.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Lead name carried on the response so the Flutter list card can show
     * the customer's name instead of leaking the internal {@code leadId}
     * (regression: {@code "Lead ID: 47"} on every card). Populated by the
     * service layer in batch after the page query so we don't add an
     * N+1 lookup or a JPA association on the entity. Not persisted —
     * {@code @Transient} keeps it out of the schema.
     */
    @Transient
    private String leadName;

    /**
     * Lazy-loaded quotation items - excluded from JSON serialization to prevent lazy-loading proxy issues
     * Use explicit item loading in service layer if items are needed in responses
     */
    @JsonIgnore
    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LeadQuotationItem> items = new ArrayList<>();

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public LeadQuotation() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLeadId() {
        return leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }

    public String getQuotationNumber() {
        return quotationNumber;
    }

    public void setQuotationNumber(String quotationNumber) {
        this.quotationNumber = quotationNumber;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTaxRatePercent() {
        return taxRatePercent;
    }

    public void setTaxRatePercent(BigDecimal taxRatePercent) {
        this.taxRatePercent = taxRatePercent;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public Integer getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(Integer validityDays) {
        this.validityDays = validityDays;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPricingMode() {
        return pricingMode;
    }

    public void setPricingMode(String pricingMode) {
        this.pricingMode = pricingMode;
    }

    public BigDecimal getRatePerSqft() {
        return ratePerSqft;
    }

    public void setRatePerSqft(BigDecimal ratePerSqft) {
        this.ratePerSqft = ratePerSqft;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getLeadName() {
        return leadName;
    }

    public void setLeadName(String leadName) {
        this.leadName = leadName;
    }

    public List<LeadQuotationItem> getItems() {
        return items;
    }

    public void setItems(List<LeadQuotationItem> items) {
        this.items = items;
    }

    // Helper methods
    public void addItem(LeadQuotationItem item) {
        items.add(item);
        item.setQuotation(this);
    }

    public void removeItem(LeadQuotationItem item) {
        items.remove(item);
        item.setQuotation(null);
    }
}
