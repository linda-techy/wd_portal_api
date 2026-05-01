package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.MarketIndexSnapshot;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class MarketIndexSnapshotRepositoryTest extends TestcontainersPostgresBase {

    @Autowired private EntityManager em;
    @Autowired private MarketIndexSnapshotRepository repo;

    @Test
    void findActive_returnsEmpty_whenNoneActive() {
        persistSnapshot(LocalDate.of(2026, 4, 1), false);
        assertThat(repo.findActive()).isEmpty();
    }

    @Test
    void findActive_returnsTheActiveOne() {
        persistSnapshot(LocalDate.of(2026, 3, 1), false);
        MarketIndexSnapshot active = persistSnapshot(LocalDate.of(2026, 4, 1), true);

        Optional<MarketIndexSnapshot> found = repo.findActive();
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(active.getId());
        assertThat(found.get().getSnapshotDate()).isEqualTo(LocalDate.of(2026, 4, 1));
    }

    private MarketIndexSnapshot persistSnapshot(LocalDate date, boolean active) {
        MarketIndexSnapshot snap = new MarketIndexSnapshot();
        snap.setSnapshotDate(date);
        snap.setSteelRate(new BigDecimal("62.50"));
        snap.setCementRate(new BigDecimal("410.00"));
        snap.setSandRate(new BigDecimal("5800.00"));
        snap.setAggregateRate(new BigDecimal("1850.00"));
        snap.setTilesRate(new BigDecimal("38.00"));
        snap.setElectricalRate(new BigDecimal("92.00"));
        snap.setPaintsRate(new BigDecimal("285.00"));
        snap.setWeightsJson(Map.<String, Object>of("steel", "0.30"));
        snap.setCompositeIndex(new BigDecimal("1.0000"));
        snap.setActive(active);
        em.persist(snap);
        em.flush();
        return snap;
    }
}
