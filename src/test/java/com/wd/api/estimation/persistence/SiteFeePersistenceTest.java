package com.wd.api.estimation.persistence;

import com.wd.api.estimation.domain.SiteFee;
import com.wd.api.estimation.domain.enums.SiteFeeMode;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SiteFeePersistenceTest extends TestcontainersPostgresBase {

    @Autowired
    private EntityManager em;

    @Test
    void roundTrip_lumpSiteFee() {
        SiteFee fee = new SiteFee();
        fee.setName("Difficult-soil surcharge");
        fee.setMode(SiteFeeMode.LUMP);
        fee.setLumpAmount(new BigDecimal("75000.00"));
        fee.setActive(true);
        em.persist(fee);
        em.flush();
        UUID id = fee.getId();
        em.clear();

        SiteFee loaded = em.find(SiteFee.class, id);
        assertThat(loaded.getMode()).isEqualTo(SiteFeeMode.LUMP);
        assertThat(loaded.getLumpAmount()).isEqualByComparingTo("75000.00");
        assertThat(loaded.getPerSqftRate()).isNull();
    }

    @Test
    void roundTrip_perSqftSiteFee() {
        SiteFee fee = new SiteFee();
        fee.setName("Excavation");
        fee.setMode(SiteFeeMode.PER_SQFT);
        fee.setPerSqftRate(new BigDecimal("12.00"));
        fee.setActive(true);
        em.persist(fee);
        em.flush();
        UUID id = fee.getId();
        em.clear();

        SiteFee loaded = em.find(SiteFee.class, id);
        assertThat(loaded.getMode()).isEqualTo(SiteFeeMode.PER_SQFT);
        assertThat(loaded.getPerSqftRate()).isEqualByComparingTo("12.00");
        assertThat(loaded.getLumpAmount()).isNull();
    }
}
