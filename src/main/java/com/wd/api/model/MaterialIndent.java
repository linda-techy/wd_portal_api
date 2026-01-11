package com.wd.api.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "material_indents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialIndent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indent_number", nullable = false, unique = true)
    private String indentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Column(name = "required_date", nullable = false)
    private LocalDate requiredDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IndentStatus status = IndentStatus.DRAFT;

    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IndentPriority priority = IndentPriority.MEDIUM;

    @Column(name = "notes")
    private String notes;

    @Column(name = "requested_by_id")
    private Long requestedById; // Often linked to PortalUser

    @Column(name = "approved_by_id")
    private Long approvedById;

    @Column(name = "approved_at")
    private java.time.LocalDateTime approvedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @OneToMany(mappedBy = "indent", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<MaterialIndentItem> items = new ArrayList<>();

    public enum IndentStatus {
        DRAFT,
        SUBMITTED,
        APPROVED,
        REJECTED,
        PO_CREATED, // Partially or Fully
        CLOSED
    }

    public enum IndentPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
