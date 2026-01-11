package com.wd.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_quotations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorQuotation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indent_id", nullable = false)
    private MaterialIndent indent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "quoted_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal quotedAmount;

    @Column(name = "items_included") // e.g., "All items" or "Items 1, 3, 5"
    private String itemsIncluded;

    @Column(name = "delivery_charges", precision = 15, scale = 2)
    private BigDecimal deliveryCharges;

    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "document_url")
    private String documentUrl; // Link to uploaded PDF quote

    @Column(name = "notes")
    private String notes;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QuotationStatus status = QuotationStatus.PENDING;

    @Column(name = "selected_at")
    private LocalDateTime selectedAt;

    public enum QuotationStatus {
        PENDING,
        SELECTED, // Chosen by PM for PO creation
        REJECTED,
        EXPIRED
    }
}
