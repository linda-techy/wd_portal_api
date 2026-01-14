package com.wd.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_milestones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMilestone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(nullable = false, length = 100)
    private String name; // e.g., "Foundation Stage"

    @Column(length = 500)
    private String description;

    @Column(name = "milestone_percentage", precision = 5, scale = 2)
    private BigDecimal milestonePercentage; // % of total project value

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Builder.Default
    @Column(length = 20, nullable = false)
    private String status = "PENDING"; // PENDING, IN_PROGRESS, COMPLETED, INVOICED

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    // Link to the generated invoice
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private ProjectInvoice invoice;

    // Progress tracking fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private MilestoneTemplate template;

    @Column(name = "completion_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal completionPercentage = BigDecimal.ZERO;

    @Column(name = "weight_percentage", precision = 5, scale = 2)
    private BigDecimal weightPercentage;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
