package com.wd.api.model;

import com.wd.api.model.enums.VariationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_variations")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectVariation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "estimated_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal estimatedAmount;

    @Builder.Default
    @Column(name = "client_approved")
    private Boolean clientApproved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private PortalUser approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private VariationStatus status = VariationStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (status == null) {
            status = VariationStatus.DRAFT;
        }
    }
}
