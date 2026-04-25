package com.wd.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "delay_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    private ProjectPhase phase;

    @Column(name = "delay_type", nullable = false, length = 50)
    private String delayType; // WEATHER, LABOUR_STRIKE, MATERIAL_DELAY, CLIENT_APPROVAL, OTHER

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "reason_text", columnDefinition = "TEXT")
    private String reasonText;

    @Column(name = "reason_category", length = 50)
    private String reasonCategory; // WEATHER, MATERIAL_SHORTAGE, LABOUR, PERMITS, CLIENT_DECISION, SUBCONTRACTOR, DESIGN_CHANGES, OTHER

    @Column(name = "responsible_party")
    private String responsibleParty;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "impact_description", columnDefinition = "TEXT")
    private String impactDescription;

    /**
     * Curated fields for customer-facing display. The raw fields above
     * (reason_text, responsible_party, impact_description) are internal-only.
     */
    @lombok.Builder.Default
    @Column(name = "customer_visible", nullable = false)
    private boolean customerVisible = false;

    @Column(name = "customer_summary", columnDefinition = "TEXT")
    private String customerSummary;

    /** NONE | MINOR | MATERIAL — structured impact on handover date. */
    @Column(name = "impact_on_handover", length = 20)
    private String impactOnHandover;

    @Column(name = "reported_by")
    private Long reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logged_by_id")
    private PortalUser loggedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
