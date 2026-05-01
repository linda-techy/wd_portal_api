package com.wd.api.estimation.repository;

import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test that every estimation repository bean is wired by Spring Data.
 * If any interface is missing or has the wrong type signature, this test fails on context startup.
 * Includes the two custom-query repositories — a malformed @Query in either would surface here
 * as a context-startup failure rather than only at first call site.
 */
class EstimationRepositoriesWiringTest extends TestcontainersPostgresBase {

    @Autowired EstimationPackageRepository packageRepo;
    @Autowired CustomisationCategoryRepository categoryRepo;
    @Autowired CustomisationOptionRepository optionRepo;
    @Autowired PackageDefaultCustomisationRepository defaultRepo;
    @Autowired AddonRepository addonRepo;
    @Autowired SiteFeeRepository siteFeeRepo;
    @Autowired GovtFeeRepository govtFeeRepo;
    @Autowired EstimationRepository estimationRepo;
    @Autowired PackageRateVersionRepository rateVersionRepo;
    @Autowired MarketIndexSnapshotRepository marketIndexRepo;

    @Test
    void allRepositories_are_wired() {
        assertThat(packageRepo).isNotNull();
        assertThat(categoryRepo).isNotNull();
        assertThat(optionRepo).isNotNull();
        assertThat(defaultRepo).isNotNull();
        assertThat(addonRepo).isNotNull();
        assertThat(siteFeeRepo).isNotNull();
        assertThat(govtFeeRepo).isNotNull();
        assertThat(estimationRepo).isNotNull();
        assertThat(rateVersionRepo).isNotNull();
        assertThat(marketIndexRepo).isNotNull();
    }
}
