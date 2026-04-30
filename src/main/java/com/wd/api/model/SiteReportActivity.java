package com.wd.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One activity row attached to a {@link SiteReport} (V84).
 *
 * <p>Models the reality that a single day's report can cover multiple
 * concurrent work fronts — "RCC slab pour" with 8 labourers, "Plastering"
 * with 4, "MEP rough-in" with 2 — instead of forcing a single rolled-up
 * manpower count. The customer app sums {@link #manpower} across rows
 * for surfaces that still want a single number.
 *
 * <p>Replace-all semantics: the service deletes existing rows for a
 * report and inserts the new set on each save. Cleaner than diff-based
 * PUT/DELETE for an MVP and avoids stale-row drift.
 */
@Entity
@Table(name = "site_report_activities")
public class SiteReportActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private SiteReport report;

    @Column(nullable = false, length = 150)
    private String name;

    @Column
    private Integer manpower;

    @Column(columnDefinition = "TEXT")
    private String equipment;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public SiteReportActivity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SiteReport getReport() { return report; }
    public void setReport(SiteReport report) { this.report = report; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getManpower() { return manpower; }
    public void setManpower(Integer manpower) { this.manpower = manpower; }

    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
