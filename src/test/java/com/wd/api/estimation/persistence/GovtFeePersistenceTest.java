package com.wd.api.estimation.persistence;

import com.wd.api.estimation.domain.GovtFee;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class GovtFeePersistenceTest extends TestcontainersPostgresBase {

    @Autowired
    private EntityManager em;

    @Test
    void roundTrip_govtFee() {
        GovtFee fee = new GovtFee();
        fee.setName("Building permit");
        fee.setLumpAmount(new BigDecimal("25000.00"));
        fee.setActive(true);
        em.persist(fee);
        em.flush();
        UUID id = fee.getId();
        em.clear();

        GovtFee loaded = em.find(GovtFee.class, id);
        assertThat(loaded.getName()).isEqualTo("Building permit");
        assertThat(loaded.getLumpAmount()).isEqualByComparingTo("25000.00");
    }
}
