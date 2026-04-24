package com.wd.api.service.wbs;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ProgressRollupServiceTest {

    private final ProgressRollupService service = new ProgressRollupService();

    @Test
    void equalWeightedTaskAverage() {
        assertThat(service.rollupMilestone(List.of(
                new ProgressRollupService.TaskInput(50, 1),
                new ProgressRollupService.TaskInput(100, 1),
                new ProgressRollupService.TaskInput(0, 1)
        )).intValue()).isEqualTo(50);
    }

    @Test
    void weightedTaskAverageRespectsWeights() {
        assertThat(service.rollupMilestone(List.of(
                new ProgressRollupService.TaskInput(100, 3),
                new ProgressRollupService.TaskInput(0, 1)
        )).intValue()).isEqualTo(75);
    }

    @Test
    void emptyMilestoneReturnsZero() {
        assertThat(service.rollupMilestone(List.of()).intValue()).isEqualTo(0);
    }

    @Test
    void allCompletedReturns100() {
        assertThat(service.rollupMilestone(List.of(
                new ProgressRollupService.TaskInput(100, 1),
                new ProgressRollupService.TaskInput(100, 5)
        )).intValue()).isEqualTo(100);
    }

    @Test
    void manualOverrideOnMilestoneBypassesRollup() {
        assertThat(service.rollupProject(List.of(
                new ProgressRollupService.MilestoneInput(BigDecimal.valueOf(50), BigDecimal.valueOf(40), "MANUAL"),
                new ProgressRollupService.MilestoneInput(BigDecimal.valueOf(0), BigDecimal.valueOf(60), "COMPUTED")
        )).intValue()).isEqualTo(20);
    }

    @Test
    void projectRollupNormalizesWeightsSummingBelow100() {
        assertThat(service.rollupProject(List.of(
                new ProgressRollupService.MilestoneInput(BigDecimal.valueOf(60), BigDecimal.valueOf(30), "COMPUTED"),
                new ProgressRollupService.MilestoneInput(BigDecimal.valueOf(40), BigDecimal.valueOf(20), "COMPUTED")
        )).intValue()).isCloseTo(52, within(1));
    }
}
