package com.wd.api.estimation.persistence;

import com.wd.api.estimation.domain.CustomisationCategory;
import com.wd.api.estimation.domain.enums.PricingMode;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class CustomisationCategoryPersistenceTest extends TestcontainersPostgresBase {

    @Autowired
    private EntityManager em;

    @Test
    void roundTrip_customisationCategory() {
        CustomisationCategory cat = new CustomisationCategory();
        cat.setName("Flooring");
        cat.setPricingMode(PricingMode.PER_SQFT);
        cat.setDisplayOrder(10);

        em.persist(cat);
        em.flush();
        UUID id = cat.getId();
        em.clear();

        CustomisationCategory loaded = em.find(CustomisationCategory.class, id);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("Flooring");
        assertThat(loaded.getPricingMode()).isEqualTo(PricingMode.PER_SQFT);
        assertThat(loaded.getDisplayOrder()).isEqualTo(10);
    }
}
