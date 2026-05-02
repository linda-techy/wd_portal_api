package com.wd.api.estimation.service;

import com.wd.api.estimation.domain.Addon;
import com.wd.api.estimation.domain.CustomisationCategory;
import com.wd.api.estimation.domain.CustomisationOption;
import com.wd.api.estimation.domain.GovtFee;
import com.wd.api.estimation.domain.SiteFee;
import com.wd.api.estimation.domain.enums.PricingMode;
import com.wd.api.estimation.domain.enums.SiteFeeMode;
import com.wd.api.estimation.dto.EstimationOptionsResponse;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class EstimationOptionsServiceTest extends TestcontainersPostgresBase {

    @Autowired
    private EntityManager em;

    @Autowired
    private EstimationOptionsService service;

    /**
     * Inserts minimal V101-equivalent fixtures then asserts the service returns
     * non-empty lists for all 4 catalog sections.
     */
    @Test
    void listOptions_v101SeedShape_returnsNonEmptyAllSections() {
        // --- Customisation category + option ---
        CustomisationCategory category = new CustomisationCategory();
        category.setName("Flooring");
        category.setPricingMode(PricingMode.PER_SQFT);
        category.setDisplayOrder(10);
        em.persist(category);
        em.flush();

        CustomisationOption option = new CustomisationOption();
        option.setCategoryId(category.getId());
        option.setName("Vitrified Tiles");
        option.setRate(new BigDecimal("180.00"));
        option.setDisplayOrder(10);
        em.persist(option);

        // --- Addon ---
        Addon addon = new Addon();
        addon.setName("Solar 3kW");
        addon.setDescription("Rooftop solar, 3kW with inverter");
        addon.setLumpAmount(new BigDecimal("180000.00"));
        addon.setActive(true);
        em.persist(addon);

        // --- Site fee ---
        SiteFee siteFee = new SiteFee();
        siteFee.setName("Difficult-soil surcharge");
        siteFee.setMode(SiteFeeMode.LUMP);
        siteFee.setLumpAmount(new BigDecimal("75000.00"));
        siteFee.setActive(true);
        em.persist(siteFee);

        // --- Govt fee ---
        GovtFee govtFee = new GovtFee();
        govtFee.setName("Building permit");
        govtFee.setLumpAmount(new BigDecimal("25000.00"));
        govtFee.setActive(true);
        em.persist(govtFee);

        em.flush();

        // --- Act ---
        EstimationOptionsResponse response = service.listOptions(null);

        // --- Assert ---
        assertThat(response.customisationCategories())
                .as("At least 1 customisation category expected")
                .isNotEmpty();

        assertThat(response.customisationCategories().get(0).options())
                .as("Category should have at least 1 option")
                .isNotEmpty();

        assertThat(response.addons())
                .as("At least 1 addon expected")
                .isNotEmpty();

        assertThat(response.siteFees())
                .as("At least 1 site fee expected")
                .isNotEmpty();

        assertThat(response.govtFees())
                .as("At least 1 govt fee expected")
                .isNotEmpty();
    }
}
