package com.wd.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wage_sheet_entries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WageSheetEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wage_sheet_id", nullable = false)
    private WageSheet wageSheet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "labour_id", nullable = false)
    private Labour labour;

    @Column(name = "days_worked", nullable = false, precision = 4, scale = 1)
    private BigDecimal daysWorked;

    @Column(name = "daily_wage", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyWage;

    @Column(name = "total_wage", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalWage;

    @Column(name = "advances_deducted", nullable = false, precision = 15, scale = 2)
    private BigDecimal advancesDeducted;

    @Column(name = "net_payable", nullable = false, precision = 15, scale = 2)
    private BigDecimal netPayable;
}
