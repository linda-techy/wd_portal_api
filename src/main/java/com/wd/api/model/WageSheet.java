package com.wd.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wage_sheets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WageSheet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_number", nullable = false, unique = true)
    private String sheetNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SheetStatus status = SheetStatus.DRAFT;

    @Builder.Default
    @OneToMany(mappedBy = "wageSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WageSheetEntry> entries = new ArrayList<>();

    public enum SheetStatus {
        DRAFT, APPROVED, PAID
    }
}
