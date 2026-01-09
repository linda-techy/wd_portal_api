package com.wd.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "measurement_book")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MeasurementBook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "labour_id")
    private Labour labour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boq_item_id")
    private BoqItem boqItem;

    @Column(nullable = false)
    private String description;

    @Column(name = "measurement_date", nullable = false)
    private LocalDate measurementDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal length;

    @Column(precision = 10, scale = 2)
    private BigDecimal breadth;

    @Column(precision = 10, scale = 2)
    private BigDecimal depth; // Height/Depth

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private String unit;

    @Column(precision = 15, scale = 2)
    private BigDecimal rate;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Override
    @PrePersist
    public void onCreate() {
        super.onCreate();
    }
}
