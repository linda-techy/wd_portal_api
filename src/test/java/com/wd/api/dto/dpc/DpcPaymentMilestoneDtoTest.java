package com.wd.api.dto.dpc;

import com.wd.api.model.PaymentStage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DpcPaymentMilestoneDto#fromStages(List)}.
 *
 * <p>{@link PaymentStage#getStagePercentage()} is stored in the DB as a decimal
 * fraction (e.g. {@code 0.30} represents 30%) — the same convention as
 * {@code retentionPct} and {@code gstRate}. The DTO is the renderer's contract,
 * so it MUST emit percent units (e.g. {@code 30.00}) so the renderer can simply
 * format without re-implementing the conversion.
 *
 * <p>Regression: a DPC issued for project 47 rendered milestones as "0%" and
 * "1%" because the DTO leaked decimal fractions through and the renderer
 * called {@code setScale(0)} on them.
 */
class DpcPaymentMilestoneDtoTest {

    @Test
    void fromStages_convertsDecimalFractionStageToPercentUnit() {
        PaymentStage s = stage(1, "Foundation", new BigDecimal("0.3000"), new BigDecimal("3540000"));

        List<DpcPaymentMilestoneDto> result = DpcPaymentMilestoneDto.fromStages(List.of(s));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).stagePercentage())
                .isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void fromStages_runningCumulativeReachesOneHundredPercent() {
        PaymentStage s1 = stage(1, "Foundation", new BigDecimal("0.3000"), new BigDecimal("3540000"));
        PaymentStage s2 = stage(2, "Completion", new BigDecimal("0.7000"), new BigDecimal("8260000"));

        List<DpcPaymentMilestoneDto> result = DpcPaymentMilestoneDto.fromStages(List.of(s1, s2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).stagePercentage()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(result.get(0).cumulativePercentage()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(result.get(1).stagePercentage()).isEqualByComparingTo(new BigDecimal("70.00"));
        assertThat(result.get(1).cumulativePercentage()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void fromStages_handlesNullStagePercentageAsZero() {
        PaymentStage s = stage(1, "Foundation", null, new BigDecimal("3540000"));

        List<DpcPaymentMilestoneDto> result = DpcPaymentMilestoneDto.fromStages(List.of(s));

        assertThat(result).hasSize(1);
        // Null stagePercentage on the entity surfaces as zero in the DTO so the
        // renderer doesn't have to null-check.
        assertThat(result.get(0).stagePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.get(0).cumulativePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void fromStages_returnsEmptyListForNullOrEmptyInput() {
        assertThat(DpcPaymentMilestoneDto.fromStages(null)).isEmpty();
        assertThat(DpcPaymentMilestoneDto.fromStages(List.of())).isEmpty();
    }

    private static PaymentStage stage(int number, String name, BigDecimal stagePctFraction, BigDecimal amountInclGst) {
        PaymentStage s = new PaymentStage();
        s.setStageNumber(number);
        s.setStageName(name);
        s.setStagePercentage(stagePctFraction);
        s.setStageAmountInclGst(amountInclGst);
        return s;
    }
}
