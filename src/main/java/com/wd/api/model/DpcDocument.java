package com.wd.api.model;

import com.wd.api.model.enums.DpcDocumentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Detailed Project Costing (DPC) document — per-project, per-revision instance.
 *
 * A DPC is a render layer over an APPROVED BoQ + the project's payment schedule.
 * Cost numbers live elsewhere; this row stores only the customer-facing choices,
 * contact overrides, and the issue-time snapshot.
 *
 * Status flow: DRAFT -> ISSUED (terminal). Once ISSUED the document is locked
 * and the rendered PDF is persisted via {@code issuedPdfDocumentId}.
 */
@SQLDelete(sql = "UPDATE dpc_document SET deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "dpc_document")
public class DpcDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boq_document_id", nullable = false)
    private BoqDocument boqDocument;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DpcDocumentStatus status = DpcDocumentStatus.DRAFT;

    // --- Title / subtitle overrides (fall back to project / template values) ---

    @Column(name = "title_override", length = 255)
    private String titleOverride;

    @Column(name = "subtitle_override", columnDefinition = "TEXT")
    private String subtitleOverride;

    // --- Signatories ---

    @Column(name = "client_signatory_name", length = 255)
    private String clientSignatoryName;

    @Column(name = "walldot_signatory_name", length = 255)
    private String walldotSignatoryName;

    // --- Walldot contacts shown on the cover / contact page ---

    @Column(name = "project_engineer_user_id")
    private Long projectEngineerUserId;

    @Column(name = "branch_manager_name", length = 255)
    private String branchManagerName;

    @Column(name = "branch_manager_phone", length = 30)
    private String branchManagerPhone;

    @Column(name = "crm_team_name", length = 255)
    private String crmTeamName;

    @Column(name = "crm_team_phone", length = 30)
    private String crmTeamPhone;

    // --- Issue snapshot ---

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "issued_by_user_id")
    private Long issuedByUserId;

    /** FK to the persisted PDF document row generated at issue time. */
    @Column(name = "issued_pdf_document_id")
    private Long issuedPdfDocumentId;

    // --- Children ---

    @OneToMany(mappedBy = "dpcDocument", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DpcDocumentScope> scopes = new ArrayList<>();

    @OneToMany(mappedBy = "dpcDocument", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DpcCustomizationLine> customizationLines = new ArrayList<>();

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (revisionNumber == null) revisionNumber = 1;
        if (status == null) status = DpcDocumentStatus.DRAFT;
    }

    // ---- Status helpers ----

    @Transient
    public boolean isDraft() { return DpcDocumentStatus.DRAFT == status; }

    @Transient
    public boolean isIssued() { return DpcDocumentStatus.ISSUED == status; }

    /** Once issued the DPC is permanently locked — no edits permitted. */
    @Transient
    public boolean isLocked() { return isIssued(); }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerProject getProject() { return project; }
    public void setProject(CustomerProject project) { this.project = project; }

    public BoqDocument getBoqDocument() { return boqDocument; }
    public void setBoqDocument(BoqDocument boqDocument) { this.boqDocument = boqDocument; }

    public Integer getRevisionNumber() { return revisionNumber; }
    public void setRevisionNumber(Integer revisionNumber) { this.revisionNumber = revisionNumber; }

    public DpcDocumentStatus getStatus() { return status; }
    public void setStatus(DpcDocumentStatus status) { this.status = status; }

    public String getTitleOverride() { return titleOverride; }
    public void setTitleOverride(String titleOverride) { this.titleOverride = titleOverride; }

    public String getSubtitleOverride() { return subtitleOverride; }
    public void setSubtitleOverride(String subtitleOverride) { this.subtitleOverride = subtitleOverride; }

    public String getClientSignatoryName() { return clientSignatoryName; }
    public void setClientSignatoryName(String clientSignatoryName) { this.clientSignatoryName = clientSignatoryName; }

    public String getWalldotSignatoryName() { return walldotSignatoryName; }
    public void setWalldotSignatoryName(String walldotSignatoryName) { this.walldotSignatoryName = walldotSignatoryName; }

    public Long getProjectEngineerUserId() { return projectEngineerUserId; }
    public void setProjectEngineerUserId(Long projectEngineerUserId) { this.projectEngineerUserId = projectEngineerUserId; }

    public String getBranchManagerName() { return branchManagerName; }
    public void setBranchManagerName(String branchManagerName) { this.branchManagerName = branchManagerName; }

    public String getBranchManagerPhone() { return branchManagerPhone; }
    public void setBranchManagerPhone(String branchManagerPhone) { this.branchManagerPhone = branchManagerPhone; }

    public String getCrmTeamName() { return crmTeamName; }
    public void setCrmTeamName(String crmTeamName) { this.crmTeamName = crmTeamName; }

    public String getCrmTeamPhone() { return crmTeamPhone; }
    public void setCrmTeamPhone(String crmTeamPhone) { this.crmTeamPhone = crmTeamPhone; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public Long getIssuedByUserId() { return issuedByUserId; }
    public void setIssuedByUserId(Long issuedByUserId) { this.issuedByUserId = issuedByUserId; }

    public Long getIssuedPdfDocumentId() { return issuedPdfDocumentId; }
    public void setIssuedPdfDocumentId(Long issuedPdfDocumentId) { this.issuedPdfDocumentId = issuedPdfDocumentId; }

    public List<DpcDocumentScope> getScopes() { return scopes; }
    public void setScopes(List<DpcDocumentScope> scopes) { this.scopes = scopes; }

    public List<DpcCustomizationLine> getCustomizationLines() { return customizationLines; }
    public void setCustomizationLines(List<DpcCustomizationLine> customizationLines) { this.customizationLines = customizationLines; }
}
