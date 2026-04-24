package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class TemplateApplyServiceTest {

    private final ProjectMilestoneRepository milestoneRepo = Mockito.mock(ProjectMilestoneRepository.class);
    private final TaskRepository taskRepo = Mockito.mock(TaskRepository.class);
    private final ProjectTypeTemplateRepository templateRepo = Mockito.mock(ProjectTypeTemplateRepository.class);
    private final MilestoneTemplateRepository milestoneTemplateRepo = Mockito.mock(MilestoneTemplateRepository.class);
    private final MilestoneTemplateTaskRepository milestoneTaskRepo = Mockito.mock(MilestoneTemplateTaskRepository.class);
    private final CustomerProjectRepository projectRepo = Mockito.mock(CustomerProjectRepository.class);
    private final TemplateApplyService service = new TemplateApplyService(
            milestoneRepo, taskRepo, templateRepo, milestoneTemplateRepo, milestoneTaskRepo, projectRepo);

    @Test
    void invalidTemplateCodeThrows() {
        Mockito.when(templateRepo.findByCode("BOGUS")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.apply(1L, "BOGUS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template not found");
    }

    @Test
    void noOpWhenProjectAlreadyHasMilestones() {
        ProjectTypeTemplate template = new ProjectTypeTemplate();
        template.setId(10L);
        template.setCode("SINGLE_FLOOR");
        Mockito.when(templateRepo.findByCode("SINGLE_FLOOR")).thenReturn(Optional.of(template));
        Mockito.when(milestoneRepo.countByProjectId(1L)).thenReturn(5L);

        TemplateApplyService.Result result = service.apply(1L, "SINGLE_FLOOR");

        assertThat(result.created()).isFalse();
        assertThat(result.milestoneCount()).isEqualTo(5);
        Mockito.verify(milestoneRepo, Mockito.never()).save(any());
    }

    @Test
    void appliesSingleFloorCreatesMilestonesAndTasks() {
        ProjectTypeTemplate template = new ProjectTypeTemplate();
        template.setId(10L);
        template.setCode("SINGLE_FLOOR");
        Mockito.when(templateRepo.findByCode("SINGLE_FLOOR")).thenReturn(Optional.of(template));
        Mockito.when(milestoneRepo.countByProjectId(1L)).thenReturn(0L);

        CustomerProject project = new CustomerProject();
        project.setId(1L);
        project.setFloors(1);
        Mockito.when(projectRepo.findById(1L)).thenReturn(Optional.of(project));

        MilestoneTemplate mt1 = mt(100L, "Site Preparation", BigDecimal.valueOf(5));
        MilestoneTemplate mt2 = mt(101L, "Foundation", BigDecimal.valueOf(12));
        Mockito.when(milestoneTemplateRepo.findByTemplateIdOrderByMilestoneOrderAsc(10L))
                .thenReturn(List.of(mt1, mt2));
        Mockito.when(milestoneTaskRepo.findByMilestoneTemplateIdOrderByTaskOrderAsc(any()))
                .thenReturn(List.of());
        Mockito.when(milestoneRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TemplateApplyService.Result result = service.apply(1L, "SINGLE_FLOOR");

        assertThat(result.created()).isTrue();
        assertThat(result.milestoneCount()).isEqualTo(2);
        Mockito.verify(milestoneRepo, Mockito.times(2)).save(any());
    }

    @Test
    void gPlusNClonesPerFloorMilestonesByFloors() {
        ProjectTypeTemplate template = new ProjectTypeTemplate();
        template.setId(20L);
        template.setCode("G_PLUS_N");
        Mockito.when(templateRepo.findByCode("G_PLUS_N")).thenReturn(Optional.of(template));
        Mockito.when(milestoneRepo.countByProjectId(1L)).thenReturn(0L);

        CustomerProject project = new CustomerProject();
        project.setId(1L);
        project.setFloors(2);
        Mockito.when(projectRepo.findById(1L)).thenReturn(Optional.of(project));

        MilestoneTemplate floorWalls = mt(200L, "Floor Walls", BigDecimal.valueOf(10));
        MilestoneTemplate handover = mt(201L, "Handover", BigDecimal.valueOf(6));
        Mockito.when(milestoneTemplateRepo.findByTemplateIdOrderByMilestoneOrderAsc(20L))
                .thenReturn(List.of(floorWalls, handover));
        Mockito.when(milestoneTaskRepo.findByMilestoneTemplateIdOrderByTaskOrderAsc(any()))
                .thenReturn(List.of());
        Mockito.when(milestoneRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TemplateApplyService.Result result = service.apply(1L, "G_PLUS_N");

        // Floor Walls is "Floor *" → cloned 3 times (Ground + 1st + 2nd) = 3
        // Handover (not Floor *) → 1
        // Total: 4
        assertThat(result.created()).isTrue();
        assertThat(result.milestoneCount()).isEqualTo(4);
    }

    private MilestoneTemplate mt(Long id, String name, BigDecimal pct) {
        MilestoneTemplate m = new MilestoneTemplate();
        m.setId(id);
        m.setMilestoneName(name);
        m.setDefaultPercentage(pct);
        return m;
    }
}
