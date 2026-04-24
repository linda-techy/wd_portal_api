package com.wd.api.seed;

import com.wd.api.model.MilestoneTemplate;
import com.wd.api.repository.MilestoneTemplateRepository;
import com.wd.api.repository.MilestoneTemplateTaskRepository;
import com.wd.api.repository.ProjectTypeTemplateRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectTypeTemplateSeedTest extends TestcontainersPostgresBase {

    @Autowired private ProjectTypeTemplateRepository templateRepo;
    @Autowired private MilestoneTemplateRepository milestoneTemplateRepo;
    @Autowired private MilestoneTemplateTaskRepository milestoneTaskRepo;

    @Test
    void allFourTemplatesExist() {
        for (String code : List.of("SINGLE_FLOOR", "G_PLUS_N", "COMMERCIAL", "INTERIOR")) {
            assertThat(templateRepo.findByCode(code))
                    .as("template %s exists", code)
                    .isPresent();
        }
    }

    @Test
    void milestoneWeightsSumToOneHundredPerTemplate() {
        for (String code : List.of("SINGLE_FLOOR", "G_PLUS_N", "COMMERCIAL", "INTERIOR")) {
            Long templateId = templateRepo.findByCode(code).orElseThrow().getId();
            List<MilestoneTemplate> milestones = milestoneTemplateRepo.findByTemplateIdOrderByMilestoneOrderAsc(templateId);
            BigDecimal sum = milestones.stream()
                    .map(MilestoneTemplate::getDefaultPercentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum.doubleValue())
                    .as("weights sum for template %s", code)
                    .isCloseTo(100.0, org.assertj.core.data.Offset.offset(1.0));
        }
    }

    @Test
    void everyMilestoneHasAtLeastOneTask() {
        for (String code : List.of("SINGLE_FLOOR", "G_PLUS_N", "COMMERCIAL", "INTERIOR")) {
            Long templateId = templateRepo.findByCode(code).orElseThrow().getId();
            for (MilestoneTemplate m : milestoneTemplateRepo.findByTemplateIdOrderByMilestoneOrderAsc(templateId)) {
                assertThat(milestoneTaskRepo.findByMilestoneTemplateIdOrderByTaskOrderAsc(m.getId()))
                        .as("tasks for milestone %s in %s", m.getMilestoneName(), code)
                        .isNotEmpty();
            }
        }
    }
}
