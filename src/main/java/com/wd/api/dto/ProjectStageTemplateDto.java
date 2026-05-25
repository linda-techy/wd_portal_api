package com.wd.api.dto;

import com.wd.api.model.ProjectStageTemplate;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for reading or writing a per-project payment-stage template (audit P2-4).
 *
 * Percentages are decimal fractions (0.10 = 10%). All stages must sum to 1.0
 * when used as a PUT/update request. GET responses reflect the stored values.
 */
public class ProjectStageTemplateDto {

    /** One stage within the template. */
    public record StageRow(
            int stageNumber,
            String name,
            BigDecimal percentage,
            String milestoneDescription
    ) {
        public static StageRow from(ProjectStageTemplate entity) {
            return new StageRow(
                    entity.getStageNumber(),
                    entity.getName(),
                    entity.getPercentage(),
                    entity.getMilestoneDescription()
            );
        }
    }

    /** Full template response (GET). */
    public record Response(Long projectId, List<StageRow> stages) {}

    /** Request body for PUT — list of stage rows that must sum to 1.0. */
    public record Request(List<StageRow> stages) {}
}
