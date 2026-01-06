package com.wd.api.model;

import com.wd.api.model.enums.PurchaseOrderStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", unique = true, nullable = false, length = 50)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "po_date", nullable = false)
    private LocalDate poDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "gst_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal gstAmount;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items;

    // ==================== Audit Trail Fields ====================

    /**
     * User who created this purchase order
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private PortalUser createdByUser;

    /**
     * User who last updated this purchase order
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private PortalUser updatedByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Soft delete timestamp
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * User who soft-deleted this purchase order
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_user_id")
    private PortalUser deletedByUser;

    /**
     * Version for optimistic locking
     */
    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null)
            status = PurchaseOrderStatus.DRAFT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== Business Logic ====================

    /**
     * Check if PO is soft-deleted
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Check if PO is active (not cancelled or closed)
     */
    public boolean isActive() {
        return status.isActive();
    }

    /**
     * Check if PO can be edited
     */
    public boolean isEditable() {
        return status.isEditable() && !isDeleted();
    }

    /**
     * Check if goods can be received for this PO
     */
    public boolean canReceiveGoods() {
        return status.canReceiveGoods() && !isDeleted();
    }

    // ==================== Constructors ====================

    public PurchaseOrder() {
    }

    // ==================== Getters & Setters ====================

    public Long getId() {
        return id;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
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

    public LocalDate getPoDate() {
        return poDate;
    }

    public void setPoDate(LocalDate poDate) {
        this.poDate = poDate;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getGstAmount() {
        return gstAmount;
    }

    public void setGstAmount(BigDecimal gstAmount) {
        this.gstAmount = gstAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseOrderStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<PurchaseOrderItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseOrderItem> items) {
        this.items = items;
    }

    // ==================== Audit Trail Getters/Setters ====================

    public PortalUser getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(PortalUser createdByUser) {
        this.createdByUser = createdByUser;
    }

    public PortalUser getUpdatedByUser() {
        return updatedByUser;
    }

    public void setUpdatedByUser(PortalUser updatedByUser) {
        this.updatedByUser = updatedByUser;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public PortalUser getDeletedByUser() {
        return deletedByUser;
    }

    public void setDeletedByUser(PortalUser deletedByUser) {
        this.deletedByUser = deletedByUser;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
