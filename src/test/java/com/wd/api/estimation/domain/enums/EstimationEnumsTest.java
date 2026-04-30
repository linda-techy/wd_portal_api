package com.wd.api.estimation.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstimationEnumsTest {

    @Test
    void projectType_values() {
        assertThat(ProjectType.values())
                .containsExactly(
                        ProjectType.NEW_BUILD,
                        ProjectType.COMMERCIAL,
                        ProjectType.RENOVATION,
                        ProjectType.INTERIOR,
                        ProjectType.COMPOUND);
    }

    @Test
    void packageInternalName_values() {
        assertThat(PackageInternalName.values())
                .containsExactly(
                        PackageInternalName.BASIC,
                        PackageInternalName.STANDARD,
                        PackageInternalName.PREMIUM);
    }

    @Test
    void pricingMode_values() {
        assertThat(PricingMode.values())
                .containsExactly(
                        PricingMode.PER_SQFT,
                        PricingMode.PER_UNIT,
                        PricingMode.PER_RFT);
    }

    @Test
    void estimationStatus_values() {
        assertThat(EstimationStatus.values())
                .containsExactly(
                        EstimationStatus.DRAFT,
                        EstimationStatus.PENDING_APPROVAL,
                        EstimationStatus.APPROVED,
                        EstimationStatus.SENT,
                        EstimationStatus.ACCEPTED,
                        EstimationStatus.REJECTED,
                        EstimationStatus.EXPIRED);
    }

    @Test
    void lineType_values() {
        assertThat(LineType.values())
                .containsExactly(
                        LineType.BASE,
                        LineType.CUSTOMISATION,
                        LineType.SITE,
                        LineType.ADDON,
                        LineType.FLUCTUATION,
                        LineType.FEE,
                        LineType.DISCOUNT,
                        LineType.GST);
    }

    @Test
    void siteFeeMode_values() {
        assertThat(SiteFeeMode.values())
                .containsExactly(SiteFeeMode.LUMP, SiteFeeMode.PER_SQFT);
    }
}
