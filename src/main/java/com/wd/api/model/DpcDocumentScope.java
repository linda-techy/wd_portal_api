package com.wd.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

/**
 * One row per scope per DPC instance.
 *
 * Records the customer's selected option (and rationale) for a scope, plus
 * any per-document overrides of the brands / what-you-get bullets that would
 * otherwise be inherited from the {@link DpcScopeTemplate}.
 *
 * Uniqueness of (dpc_document_id, scope_template_id) is enforced at the DB level.
 */
@SQLDelete(sql = "UPDATE dpc_document_scope SET deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "dpc_document_scope")
public class DpcDocumentScope extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dpc_document_id", nullable = false)
    private DpcDocument dpcDocument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scope_template_id", nullable = false)
    private DpcScopeTemplate scopeTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private DpcScopeOption selectedOption;

    @Column(name = "selected_option_rationale", columnDefinition = "TEXT")
    private String selectedOptionRationale;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "brands_override", columnDefinition = "jsonb")
    private Map<String, String> brandsOverride;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "what_you_get_override", columnDefinition = "jsonb")
    private List<String> whatYouGetOverride;

    @Column(name = "included_in_pdf", nullable = false)
    private Boolean includedInPdf = true;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (includedInPdf == null) includedInPdf = true;
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DpcDocument getDpcDocument() { return dpcDocument; }
    public void setDpcDocument(DpcDocument dpcDocument) { this.dpcDocument = dpcDocument; }

    public DpcScopeTemplate getScopeTemplate() { return scopeTemplate; }
    public void setScopeTemplate(DpcScopeTemplate scopeTemplate) { this.scopeTemplate = scopeTemplate; }

    public DpcScopeOption getSelectedOption() { return selectedOption; }
    public void setSelectedOption(DpcScopeOption selectedOption) { this.selectedOption = selectedOption; }

    public String getSelectedOptionRationale() { return selectedOptionRationale; }
    public void setSelectedOptionRationale(String selectedOptionRationale) { this.selectedOptionRationale = selectedOptionRationale; }

    public Map<String, String> getBrandsOverride() { return brandsOverride; }
    public void setBrandsOverride(Map<String, String> brandsOverride) { this.brandsOverride = brandsOverride; }

    public List<String> getWhatYouGetOverride() { return whatYouGetOverride; }
    public void setWhatYouGetOverride(List<String> whatYouGetOverride) { this.whatYouGetOverride = whatYouGetOverride; }

    public Boolean getIncludedInPdf() { return includedInPdf; }
    public void setIncludedInPdf(Boolean includedInPdf) { this.includedInPdf = includedInPdf; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
