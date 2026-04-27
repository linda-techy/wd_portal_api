package com.wd.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DPC Scope Template — admin-managed content library row.
 *
 * One row per scope topic (Foundation, Superstructure, Plastering, ...). The
 * narrative content (intro, what-you-get, quality-procedures, documents-you-get)
 * and the matching rules used to pull cost numbers from the BoQ live here.
 *
 * Cost figures are NOT stored — DPC is a render layer over BoQ + PaymentSchedule.
 */
@SQLDelete(sql = "UPDATE dpc_scope_template SET deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "dpc_scope_template")
public class DpcScopeTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String subtitle;

    @Column(name = "intro_paragraph", columnDefinition = "TEXT")
    private String introParagraph;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "what_you_get", columnDefinition = "jsonb", nullable = false)
    private List<String> whatYouGet = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quality_procedures", columnDefinition = "jsonb", nullable = false)
    private List<String> qualityProcedures = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "documents_you_get", columnDefinition = "jsonb", nullable = false)
    private List<String> documentsYouGet = new ArrayList<>();

    /** ILIKE patterns matched against BoqCategory.name to source cost numbers. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "boq_category_patterns", columnDefinition = "jsonb", nullable = false)
    private List<String> boqCategoryPatterns = new ArrayList<>();

    /** e.g. {"Cement": "Ambuja / ACC", "Steel": "Vizag / Kaliyathu"} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_brands", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> defaultBrands = new HashMap<>();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "scopeTemplate", fetch = FetchType.LAZY)
    private List<DpcScopeOption> options = new ArrayList<>();

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (whatYouGet == null) whatYouGet = new ArrayList<>();
        if (qualityProcedures == null) qualityProcedures = new ArrayList<>();
        if (documentsYouGet == null) documentsYouGet = new ArrayList<>();
        if (boqCategoryPatterns == null) boqCategoryPatterns = new ArrayList<>();
        if (defaultBrands == null) defaultBrands = new HashMap<>();
        if (isActive == null) isActive = true;
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getIntroParagraph() { return introParagraph; }
    public void setIntroParagraph(String introParagraph) { this.introParagraph = introParagraph; }

    public List<String> getWhatYouGet() { return whatYouGet; }
    public void setWhatYouGet(List<String> whatYouGet) { this.whatYouGet = whatYouGet; }

    public List<String> getQualityProcedures() { return qualityProcedures; }
    public void setQualityProcedures(List<String> qualityProcedures) { this.qualityProcedures = qualityProcedures; }

    public List<String> getDocumentsYouGet() { return documentsYouGet; }
    public void setDocumentsYouGet(List<String> documentsYouGet) { this.documentsYouGet = documentsYouGet; }

    public List<String> getBoqCategoryPatterns() { return boqCategoryPatterns; }
    public void setBoqCategoryPatterns(List<String> boqCategoryPatterns) { this.boqCategoryPatterns = boqCategoryPatterns; }

    public Map<String, String> getDefaultBrands() { return defaultBrands; }
    public void setDefaultBrands(Map<String, String> defaultBrands) { this.defaultBrands = defaultBrands; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public List<DpcScopeOption> getOptions() { return options; }
    public void setOptions(List<DpcScopeOption> options) { this.options = options; }
}
