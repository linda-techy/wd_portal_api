package com.wd.api.estimation.persistence;

import com.wd.api.estimation.domain.MarketIndexSnapshot;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class MarketIndexSnapshotPersistenceTest extends TestcontainersPostgresBase {

    @Autowired
    private EntityManager em;

    @Test
    void roundTrip_marketIndexSnapshot() {
        MarketIndexSnapshot snap = new MarketIndexSnapshot();
        snap.setSnapshotDate(LocalDate.of(2026, 4, 30));
        snap.setSteelRate(new BigDecimal("62.50"));
        snap.setCementRate(new BigDecimal("410.00"));
        snap.setSandRate(new BigDecimal("5800.00"));
        snap.setAggregateRate(new BigDecimal("1850.00"));
        snap.setTilesRate(new BigDecimal("38.00"));
        snap.setElectricalRate(new BigDecimal("92.00"));
        snap.setPaintsRate(new BigDecimal("285.00"));
        snap.setWeightsJson(Map.<String, Object>of(
                "steel", "0.30",
                "cement", "0.20",
                "sand", "0.12",
                "aggregate", "0.08",
                "tiles", "0.12",
                "electrical", "0.10",
                "paints", "0.08"));
        snap.setCompositeIndex(new BigDecimal("1.0000"));
        snap.setActive(true);

        em.persist(snap);
        em.flush();
        UUID id = snap.getId();
        em.clear();

        MarketIndexSnapshot loaded = em.find(MarketIndexSnapshot.class, id);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getSnapshotDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(loaded.getSteelRate()).isEqualByComparingTo("62.50");
        assertThat(loaded.getCompositeIndex()).isEqualByComparingTo("1.0000");
        assertThat(loaded.isActive()).isTrue();
        assertThat(loaded.getWeightsJson())
                .containsEntry("steel", "0.30")
                .containsEntry("cement", "0.20");
    }
}
