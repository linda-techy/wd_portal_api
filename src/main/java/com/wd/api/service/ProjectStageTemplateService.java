package com.wd.api.service;

import com.wd.api.dto.ProjectStageTemplateDto;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.ProjectStageTemplate;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.ProjectStageTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Manages per-project payment-stage templates (audit P2-4).
 *
 * <h3>Invariants</h3>
 * <ul>
 *   <li>A template for a project is a list of stages whose percentages sum to
 *       exactly 1.0 (±0.001 tolerance). Any update that violates this is
 *       rejected before touching the database.</li>
 *   <li>Updating is a full replace — the existing rows are deleted and the new
 *       rows are inserted. This keeps the update contract simple and avoids
 *       partial-update inconsistencies.</li>
 * </ul>
 */
@Service
@Transactional
public class ProjectStageTemplateService {

    private final ProjectStageTemplateRepository templateRepository;
    private final CustomerProjectRepository projectRepository;

    public ProjectStageTemplateService(ProjectStageTemplateRepository templateRepository,
                                       CustomerProjectRepository projectRepository) {
        this.templateRepository = templateRepository;
        this.projectRepository = projectRepository;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProjectStageTemplateDto.Response getTemplate(Long projectId) {
        requireProject(projectId);
        List<ProjectStageTemplate> rows =
                templateRepository.findByProjectIdOrderByStageNumber(projectId);
        if (rows.isEmpty()) {
            // Lazy-seed the 10-stage residential G+1 default on first access.
            // (The V159 migration covers all existing projects; this path
            // handles projects created after the migration that somehow
            // missed the seed — belt-and-suspenders guard.)
            return setTemplate(projectId, KERALA_DEFAULT_STAGES);
        }
        List<ProjectStageTemplateDto.StageRow> stages =
                rows.stream().map(ProjectStageTemplateDto.StageRow::from).toList();
        return new ProjectStageTemplateDto.Response(projectId, stages);
    }

    /**
     * 10-stage residential G+1 default used when no template has been configured.
     * Percentages sum to exactly 1.0000.
     * Per-project count and percentages remain fully editable via setTemplate().
     */
    private static final BigDecimal PCT_10 = new BigDecimal("0.1000");

    static final List<ProjectStageTemplateDto.StageRow> KERALA_DEFAULT_STAGES = List.of(
            new ProjectStageTemplateDto.StageRow(1,  "Mobilisation / Advance",          PCT_10, "Advance payment on contract signing"),
            new ProjectStageTemplateDto.StageRow(2,  "Foundation & Footing",             PCT_10, "On completion of foundation and footing"),
            new ProjectStageTemplateDto.StageRow(3,  "Plinth Beam / DPC",                PCT_10, "On completion of plinth beam / DPC"),
            new ProjectStageTemplateDto.StageRow(4,  "Ground Floor Slab",                new BigDecimal("0.1500"), "On casting the ground-floor roof slab"),
            new ProjectStageTemplateDto.StageRow(5,  "First Floor Walls & Columns",      PCT_10, "On completion of first-floor walls and columns"),
            new ProjectStageTemplateDto.StageRow(6,  "First Floor Roof Slab",            new BigDecimal("0.1500"), "On casting the first-floor roof slab"),
            new ProjectStageTemplateDto.StageRow(7,  "Brickwork & External Plaster",     PCT_10, "On completion of brickwork and external plastering"),
            new ProjectStageTemplateDto.StageRow(8,  "Internal Plaster & Flooring",      new BigDecimal("0.0800"), "On completion of internal plastering and flooring"),
            new ProjectStageTemplateDto.StageRow(9,  "MEP, Painting & Finishing",        new BigDecimal("0.0700"), "On completion of electrical, plumbing, painting and finishing"),
            new ProjectStageTemplateDto.StageRow(10, "Handover / Completion",            new BigDecimal("0.0500"), "Final payment on project handover")
    );

    // ── Write (full replace) ──────────────────────────────────────────────────

    /**
     * Replaces the project's stage template with the supplied list.
     *
     * @throws IllegalArgumentException if percentages do not sum to 100%
     *                                  or any stage row is invalid
     */
    public ProjectStageTemplateDto.Response setTemplate(Long projectId,
                                                         List<ProjectStageTemplateDto.StageRow> stages) {
        CustomerProject project = requireProject(projectId);

        validateStages(stages);

        // Full replace: delete existing, insert fresh rows
        templateRepository.deleteByProjectId(projectId);
        templateRepository.flush();

        for (ProjectStageTemplateDto.StageRow row : stages) {
            ProjectStageTemplate entity = new ProjectStageTemplate();
            entity.setProject(project);
            entity.setStageNumber(row.stageNumber());
            entity.setName(row.name());
            entity.setPercentage(row.percentage());
            entity.setMilestoneDescription(row.milestoneDescription());
            templateRepository.save(entity);
        }

        return getTemplate(projectId);
    }

    // ── Shared validation ─────────────────────────────────────────────────────

    /**
     * Validates that stage rows are non-empty and their percentages sum to 1.0.
     * Shared by the service's setTemplate and by BoqDocumentService via this
     * public helper.
     *
     * @throws IllegalArgumentException on any validation failure
     */
    public static void validateStages(List<ProjectStageTemplateDto.StageRow> stages) {
        if (stages == null || stages.isEmpty()) {
            throw new IllegalArgumentException("At least one payment stage must be defined.");
        }
        for (ProjectStageTemplateDto.StageRow row : stages) {
            if (row.name() == null || row.name().isBlank()) {
                throw new IllegalArgumentException("Stage name must not be blank (stage " + row.stageNumber() + ").");
            }
            if (row.percentage() == null || row.percentage().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Stage percentage must be > 0 (stage " + row.stageNumber() + ").");
            }
        }
        BigDecimal total = stages.stream()
                .map(ProjectStageTemplateDto.StageRow::percentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.001")) > 0) {
            throw new IllegalArgumentException(
                    "Stage percentages must sum to 1.0 (100%). Current total: " + total);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private CustomerProject requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
    }
}
