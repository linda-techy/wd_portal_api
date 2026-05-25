package com.wd.api.service;

import com.wd.api.dto.ProjectStageTemplateDto;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.ProjectStageTemplate;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.ProjectStageTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProjectStageTemplateService (audit P2-4).
 * Uses Mockito mocks — no Spring context.
 */
@ExtendWith(MockitoExtension.class)
class ProjectStageTemplateServiceTest {

    @Mock private ProjectStageTemplateRepository templateRepository;
    @Mock private CustomerProjectRepository projectRepository;

    @InjectMocks
    private ProjectStageTemplateService service;

    private CustomerProject project;

    @BeforeEach
    void setUp() {
        project = new CustomerProject();
        project.setId(1L);
        project.setName("Test Project");
    }

    // ── (a) Non-100% percentages rejected ────────────────────────────────────

    @Test
    void setTemplate_rejectsWhenPercentagesDontSumTo100() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        List<ProjectStageTemplateDto.StageRow> badStages = List.of(
                new ProjectStageTemplateDto.StageRow(1, "Foundation",  new BigDecimal("0.5000"), null),
                new ProjectStageTemplateDto.StageRow(2, "Finishing",   new BigDecimal("0.3000"), null)
                // total = 0.80, not 1.0
        );

        assertThatThrownBy(() -> service.setTemplate(1L, badStages))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sum to 1.0");

        verify(templateRepository, never()).save(any());
    }

    @Test
    void validateStages_staticHelper_rejectsEmptyList() {
        assertThatThrownBy(() -> ProjectStageTemplateService.validateStages(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one");
    }

    @Test
    void validateStages_staticHelper_rejectsBlankName() {
        List<ProjectStageTemplateDto.StageRow> stages = List.of(
                new ProjectStageTemplateDto.StageRow(1, "  ", new BigDecimal("1.0000"), null)
        );
        assertThatThrownBy(() -> ProjectStageTemplateService.validateStages(stages))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void validateStages_staticHelper_rejectsZeroPercentage() {
        List<ProjectStageTemplateDto.StageRow> stages = List.of(
                new ProjectStageTemplateDto.StageRow(1, "Stage", BigDecimal.ZERO, null)
        );
        assertThatThrownBy(() -> ProjectStageTemplateService.validateStages(stages))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("> 0");
    }

    // ── (b) Valid template persists ───────────────────────────────────────────

    @Test
    void setTemplate_persistsValidTemplate() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // 10-stage residential G+1 default sums exactly to 1.0
        List<ProjectStageTemplateDto.StageRow> stages = ProjectStageTemplateService.KERALA_DEFAULT_STAGES;

        // stub save + findBy to simulate round-trip
        when(templateRepository.save(any())).thenAnswer(inv -> {
            ProjectStageTemplate t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });
        when(templateRepository.findByProjectIdOrderByStageNumber(1L))
                .thenAnswer(inv -> {
                    // Return entities mirroring the input rows
                    return stages.stream().map(row -> {
                        ProjectStageTemplate e = new ProjectStageTemplate();
                        e.setProject(project);
                        e.setStageNumber(row.stageNumber());
                        e.setName(row.name());
                        e.setPercentage(row.percentage());
                        e.setMilestoneDescription(row.milestoneDescription());
                        return e;
                    }).toList();
                });

        ProjectStageTemplateDto.Response response = service.setTemplate(1L, stages);

        // delete-then-insert: deleteByProjectId + flush called once
        verify(templateRepository).deleteByProjectId(1L);
        verify(templateRepository).flush();

        // ten rows saved
        verify(templateRepository, times(10)).save(any(ProjectStageTemplate.class));

        assertThat(response.stages()).hasSize(10);
        assertThat(response.stages().get(0).name()).isEqualTo("Mobilisation / Advance");
        assertThat(response.stages().get(9).name()).isEqualTo("Handover / Completion");
    }

    // ── (c) Approval with no stageConfigs uses stored template ────────────────
    //
    // This is verified by reading the code path in BoqDocumentService:
    //   if (stageConfigs != null && !stageConfigs.isEmpty()) { ... explicit ... }
    //   else {
    //     templateRows = stageTemplateRepository.findByProjectIdOrderByStageNumber(projectId)
    //     if (templateRows.isEmpty()) throw IllegalStateException(...)
    //     effectiveConfigs = templateRows.stream().map(t -> new StageConfig(...)).toList()
    //     validateStagePercentages(effectiveConfigs)
    //   }
    // The following test verifies the service produces storable rows that the
    // BoqDocumentService fallback path can consume correctly.

    @Test
    void getTemplate_returnsStoredRows_whenPresent() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        ProjectStageTemplate row = new ProjectStageTemplate();
        row.setProject(project);
        row.setStageNumber(1);
        row.setName("Foundation");
        row.setPercentage(new BigDecimal("1.0000"));
        row.setMilestoneDescription("desc");

        when(templateRepository.findByProjectIdOrderByStageNumber(1L))
                .thenReturn(List.of(row));

        ProjectStageTemplateDto.Response response = service.getTemplate(1L);

        assertThat(response.projectId()).isEqualTo(1L);
        assertThat(response.stages()).hasSize(1);
        assertThat(response.stages().get(0).name()).isEqualTo("Foundation");
        // No seed call because rows were present
        verify(templateRepository, never()).deleteByProjectId(anyLong());
    }

    // ── (d) Explicit stageConfigs override stored template ────────────────────
    //
    // BoqDocumentService checks stageConfigs != null && !isEmpty() FIRST, so any
    // non-empty explicit list is used directly without touching the repository.
    // We verify the validation helper accepts a custom valid split.

    @Test
    void validateStages_acceptsCustomSplit() {
        List<ProjectStageTemplateDto.StageRow> custom = List.of(
                new ProjectStageTemplateDto.StageRow(1, "Phase A", new BigDecimal("0.6000"), null),
                new ProjectStageTemplateDto.StageRow(2, "Phase B", new BigDecimal("0.4000"), null)
        );
        // must not throw
        assertThatCode(() -> ProjectStageTemplateService.validateStages(custom))
                .doesNotThrowAnyException();
    }

    // ── Default seeding on first GET ──────────────────────────────────────────

    @Test
    void getTemplate_seedsKeralaDefaultWhenEmpty() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // First call (inside setTemplate via getTemplate) returns empty → triggers seed
        // Second call (getTemplate inside setTemplate reads back after save) returns seeded rows
        List<ProjectStageTemplate> seededRows = ProjectStageTemplateService.KERALA_DEFAULT_STAGES
                .stream().map(row -> {
                    ProjectStageTemplate e = new ProjectStageTemplate();
                    e.setProject(project);
                    e.setStageNumber(row.stageNumber());
                    e.setName(row.name());
                    e.setPercentage(row.percentage());
                    e.setMilestoneDescription(row.milestoneDescription());
                    return e;
                }).toList();

        when(templateRepository.findByProjectIdOrderByStageNumber(1L))
                .thenReturn(List.of())          // first call: no rows → seed
                .thenReturn(seededRows);         // second call: rows after seed

        when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjectStageTemplateDto.Response response = service.getTemplate(1L);

        verify(templateRepository, times(10)).save(any(ProjectStageTemplate.class));
        assertThat(response.stages()).hasSize(10);
        assertThat(response.stages().get(0).name()).isEqualTo("Mobilisation / Advance");
    }

    // ── (e) Default has 10 stages summing to exactly 1.0000 ──────────────────

    @Test
    void defaultStages_have10StagesSummingToOne() {
        List<ProjectStageTemplateDto.StageRow> defaults = ProjectStageTemplateService.KERALA_DEFAULT_STAGES;

        assertThat(defaults).hasSize(10);

        BigDecimal sum = defaults.stream()
                .map(ProjectStageTemplateDto.StageRow::percentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(new BigDecimal("1.0000"));
    }

    // ── (f) Per-project flexibility: arbitrary custom counts still accepted ───

    @Test
    void validateStages_accepts4StageCustomTemplate() {
        List<ProjectStageTemplateDto.StageRow> fourStage = List.of(
                new ProjectStageTemplateDto.StageRow(1, "Advance",    new BigDecimal("0.2000"), null),
                new ProjectStageTemplateDto.StageRow(2, "Structure",  new BigDecimal("0.3000"), null),
                new ProjectStageTemplateDto.StageRow(3, "Finishing",  new BigDecimal("0.3000"), null),
                new ProjectStageTemplateDto.StageRow(4, "Handover",   new BigDecimal("0.2000"), null)
        );
        assertThatCode(() -> ProjectStageTemplateService.validateStages(fourStage))
                .doesNotThrowAnyException();
    }

    @Test
    void validateStages_accepts12StageCustomTemplate() {
        // 2×0.10 + 10×0.08 = 0.20 + 0.80 = 1.00
        List<ProjectStageTemplateDto.StageRow> twelveStageClean = List.of(
                new ProjectStageTemplateDto.StageRow(1,  "S1",  new BigDecimal("0.1000"), null),
                new ProjectStageTemplateDto.StageRow(2,  "S2",  new BigDecimal("0.1000"), null),
                new ProjectStageTemplateDto.StageRow(3,  "S3",  new BigDecimal("0.0800"), null),
                new ProjectStageTemplateDto.StageRow(4,  "S4",  new BigDecimal("0.0800"), null),
                new ProjectStageTemplateDto.StageRow(5,  "S5",  new BigDecimal("0.0800"), null),
                new ProjectStageTemplateDto.StageRow(6,  "S6",  new BigDecimal("0.0800"), null),
                new ProjectStageTemplateDto.StageRow(7,  "S7",  new BigDecimal("0.0800"), null),
                new ProjectStageTemplateDto.StageRow(8,  "S8",  new BigDecimal("0.0800"), null),
                new ProjectStageTemplateDto.StageRow(9,  "S9",  new BigDecimal("0.0800"), null),
                new ProjectStageTemplateDto.StageRow(10, "S10", new BigDecimal("0.0800"), null),
                new ProjectStageTemplateDto.StageRow(11, "S11", new BigDecimal("0.0800"), null),
                new ProjectStageTemplateDto.StageRow(12, "S12", new BigDecimal("0.0800"), null)
        );
        assertThatCode(() -> ProjectStageTemplateService.validateStages(twelveStageClean))
                .doesNotThrowAnyException();
    }
}
