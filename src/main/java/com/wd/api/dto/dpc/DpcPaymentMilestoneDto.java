package com.wd.api.dto.dpc;

import com.wd.api.model.PaymentStage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for one row of the DPC payment-milestones page.
 *
 * Includes a running cumulative percentage so the renderer can show a
 * "% paid by stage" column without recomputing.
 */
public record DpcPaymentMilestoneDto(
        Integer stageNumber,
        String stageName,
        BigDecimal stagePercentage,
        BigDecimal stageAmountInclGst,
        String milestoneDescription,
        BigDecimal cumulativePercentage
) {

    /**
     * Build a milestone list from {@link PaymentStage} entities, ordered by
     * stage_number, with running cumulative percentages.
     *
     * @param stages stages already ordered by stage_number ascending
     * @return milestone DTOs (never null; empty if input is null/empty)
     */
    public static List<DpcPaymentMilestoneDto> fromStages(List<PaymentStage> stages) {
        if (stages == null || stages.isEmpty()) return List.of();

        List<DpcPaymentMilestoneDto> result = new ArrayList<>(stages.size());
        BigDecimal cumulative = BigDecimal.ZERO;
        for (PaymentStage s : stages) {
            BigDecimal pct = s.getStagePercentage() != null ? s.getStagePercentage() : BigDecimal.ZERO;
            cumulative = cumulative.add(pct);
            result.add(new DpcPaymentMilestoneDto(
                    s.getStageNumber(),
                    s.getStageName(),
                    s.getStagePercentage(),
                    s.getStageAmountInclGst(),
                    s.getMilestoneDescription(),
                    cumulative
            ));
        }
        return result;
    }
}
