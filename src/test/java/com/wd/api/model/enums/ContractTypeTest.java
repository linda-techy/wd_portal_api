package com.wd.api.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ContractType.isSupportedForNewProjects().
 * Verifies that TURNKEY and ITEM_RATE are offered for new projects
 * and that LABOR_ONLY / COST_PLUS are retained for backward-compat only.
 */
class ContractTypeTest {

    @Test
    void turnkey_isSupportedForNewProjects() {
        assertThat(ContractType.TURNKEY.isSupportedForNewProjects()).isTrue();
    }

    @Test
    void itemRate_isSupportedForNewProjects() {
        assertThat(ContractType.ITEM_RATE.isSupportedForNewProjects()).isTrue();
    }

    @Test
    void laborOnly_isNotSupportedForNewProjects() {
        assertThat(ContractType.LABOR_ONLY.isSupportedForNewProjects()).isFalse();
    }

    @Test
    void costPlus_isNotSupportedForNewProjects() {
        assertThat(ContractType.COST_PLUS.isSupportedForNewProjects()).isFalse();
    }

    @Test
    void allFourConstantsStillExist_forBackwardCompat() {
        // All four constants must remain so existing project rows can be loaded by JPA.
        assertThat(ContractType.values())
                .containsExactly(
                        ContractType.TURNKEY,
                        ContractType.LABOR_ONLY,
                        ContractType.ITEM_RATE,
                        ContractType.COST_PLUS);
    }
}
