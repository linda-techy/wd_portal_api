package com.wd.api.dto.dpc;

import com.wd.api.model.PaymentStage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for one row of the DPC payment-milestones page.
 *
 * <p>{@code stagePercentage} and {@code cumulativePercentage} are expressed in
 * <strong>percent units</strong> (e.g. {@code 30.00} represents 30%). The
 * underlying {@link PaymentStage#getStagePercentage()} stores a decimal
 * fraction ({@code 0.30}); {@link #fromStages(List)} performs the conversion
 * so renderers can format the values directly.
 */
public record DpcPaymentMilestoneDto(
        Integer stageNumber,
        String stageName,
        BigDecimal stagePercentage,
        BigDecimal stageAmountInclGst,
        String milestoneDescription,
        BigDecimal cumulativePercentage
) {

    private static final BigDecimal PERCENT_FACTOR = new BigDecimal("100");

    /**
     * Build a milestone list from {@link PaymentStage} entities, ordered by
     * stage_number, with running cumulative percentages.
     *
     * <p>Decimal-fraction stage percentages from the entity (e.g. {@code 0.30})
     * are converted to percent units (e.g. {@code 30.00}) here so downstream
     * renderers don't re-implement the conversion.
     *
     * @param stages stages already ordered by stage_number ascending
     * @return milestone DTOs (never null; empty if input is null/empty)
     */
    public static List<DpcPaymentMilestoneDto> fromStages(List<PaymentStage> stages) {
        if (stages == null || stages.isEmpty()) return List.of();

        List<DpcPaymentMilestoneDto> result = new ArrayList<>(stages.size());
        BigDecimal cumulativeFraction = BigDecimal.ZERO;
        for (PaymentStage s : stages) {
            BigDecimal fraction = s.getStagePercentage() != null ? s.getStagePercentage() : BigDecimal.ZERO;
            cumulativeFraction = cumulativeFraction.add(fraction);
            result.add(new DpcPaymentMilestoneDto(
                    s.getStageNumber(),
                    s.getStageName(),
                    fraction.multiply(PERCENT_FACTOR),
                    s.getStageAmountInclGst(),
                    s.getMilestoneDescription(),
                    cumulativeFraction.multiply(PERCENT_FACTOR)
            ));
        }
        return result;
    }
}
