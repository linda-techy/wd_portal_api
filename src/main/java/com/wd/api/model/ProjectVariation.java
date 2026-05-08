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
    @Column(length = 40, nullable = false)
    private VariationStatus status = VariationStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cost_impact", precision = 15, scale = 2)
    private BigDecimal costImpact;

    @Column(name = "time_impact_working_days")
    private Integer timeImpactWorkingDays;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "costed_at")
    private LocalDateTime costedAt;

    @Column(name = "sent_to_customer_at")
    private LocalDateTime sentToCustomerAt;

    // approvedAt already declared above — left as-is.

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (status == null) {
            status = VariationStatus.DRAFT;
        }
    }
}
