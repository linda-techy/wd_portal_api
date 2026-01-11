package com.wd.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "material_indent_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialIndentItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indent_id", nullable = false)
    @JsonBackReference
    private MaterialIndent indent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(name = "item_name", nullable = false) // For ad-hoc items not in Material DB
    private String itemName;

    @Column(name = "description")
    private String description;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "quantity_requested", nullable = false)
    private BigDecimal quantityRequested;

    @Column(name = "quantity_approved")
    private BigDecimal quantityApproved;

    // Track PO status for this item
    @Column(name = "po_quantity")
    @Builder.Default
    private BigDecimal poQuantity = BigDecimal.ZERO;

    @Column(name = "estimated_rate")
    private BigDecimal estimatedRate;

    @Column(name = "estimated_amount")
    private BigDecimal estimatedAmount;
}
