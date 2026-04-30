package com.wd.api.estimation.persistence;

import com.wd.api.estimation.domain.Addon;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class AddonPersistenceTest extends TestcontainersPostgresBase {

    @Autowired
    private EntityManager em;

    @Test
    void roundTrip_addon() {
        Addon a = new Addon();
        a.setName("Solar 3kW");
        a.setDescription("Rooftop solar installation, 3kW system, includes inverter");
        a.setLumpAmount(new BigDecimal("180000.00"));
        a.setActive(true);

        em.persist(a);
        em.flush();
        UUID id = a.getId();
        em.clear();

        Addon loaded = em.find(Addon.class, id);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("Solar 3kW");
        assertThat(loaded.getLumpAmount()).isEqualByComparingTo("180000.00");
        assertThat(loaded.isActive()).isTrue();
    }
}
