package com.wd.api.estimation.persistence;

import com.wd.api.estimation.domain.CustomisationCategory;
import com.wd.api.estimation.domain.CustomisationOption;
import com.wd.api.estimation.domain.enums.PricingMode;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class CustomisationOptionPersistenceTest extends TestcontainersPostgresBase {

    @Autowired
    private EntityManager em;

    @Test
    void roundTrip_customisationOption() {
        CustomisationCategory cat = new CustomisationCategory();
        cat.setName("Flooring");
        cat.setPricingMode(PricingMode.PER_SQFT);
        em.persist(cat);
        em.flush();

        CustomisationOption opt = new CustomisationOption();
        opt.setCategoryId(cat.getId());
        opt.setName("Italian Marble");
        opt.setRate(new BigDecimal("950.00"));
        opt.setDisplayOrder(30);

        em.persist(opt);
        em.flush();
        UUID id = opt.getId();
        em.clear();

        CustomisationOption loaded = em.find(CustomisationOption.class, id);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getCategoryId()).isEqualTo(cat.getId());
        assertThat(loaded.getName()).isEqualTo("Italian Marble");
        assertThat(loaded.getRate()).isEqualByComparingTo("950.00");
    }
}
