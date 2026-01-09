package com.wd.api.model;

import com.wd.api.model.enums.WarrantyStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "project_warranties")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectWarranty extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(name = "component_name", nullable = false)
    private String componentName; // e.g., "Waterproofing", "Structure", "HVAC"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "provider_name")
    private String providerName; // e.g., "Asian Paints", "Daikin"

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private WarrantyStatus status = WarrantyStatus.ACTIVE;

    @Column(name = "coverage_details", columnDefinition = "TEXT")
    private String coverageDetails;

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (status == null)
            status = WarrantyStatus.ACTIVE;
    }
}
