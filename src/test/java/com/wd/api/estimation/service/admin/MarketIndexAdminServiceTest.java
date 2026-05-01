package com.wd.api.estimation.service.admin;

import com.wd.api.estimation.domain.MarketIndexSnapshot;
import com.wd.api.estimation.dto.admin.MarketIndexCreateRequest;
import com.wd.api.estimation.dto.admin.MarketIndexResponse;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class MarketIndexAdminServiceTest extends TestcontainersPostgresBase {

    @Autowired private EntityManager em;
    @Autowired private MarketIndexAdminService service;

    private static final Map<String, String> STANDARD_WEIGHTS = Map.of(
            "steel", "0.30",
            "cement", "0.20",
            "sand", "0.12",
            "aggregate", "0.08",
            "tiles", "0.12",
            "electrical", "0.10",
            "paints", "0.08");

    @Test
    void create_firstSnapshot_compositeIs1_0000() {
        MarketIndexCreateRequest req = new MarketIndexCreateRequest(
                null,
                new BigDecimal("62.50"), new BigDecimal("410.00"),
                new BigDecimal("5800.00"), new BigDecimal("1850.00"),
                new BigDecimal("38.00"), new BigDecimal("92.00"), new BigDecimal("285.00"),
                STANDARD_WEIGHTS);
        MarketIndexResponse resp = service.createSnapshot(req);
        assertThat(resp.compositeIndex())
                .as("First snapshot has no baseline → composite = 1.0000 by definition")
                .isEqualByComparingTo("1.0000");
        assertThat(resp.active()).isTrue();
        assertThat(resp.snapshotDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void create_secondSnapshot_4PercentSteelIncrease_compositeReflectsWeighted() {
        // Baseline
        service.createSnapshot(new MarketIndexCreateRequest(
                null, new BigDecimal("62.50"), new BigDecimal("410.00"),
                new BigDecimal("5800.00"), new BigDecimal("1850.00"),
                new BigDecimal("38.00"), new BigDecimal("92.00"), new BigDecimal("285.00"),
                STANDARD_WEIGHTS));
        em.flush();

        // Steel up 4%, all else flat. New composite =
        //   (65.00/62.50)*0.30 + 1.0*(0.20+0.12+0.08+0.12+0.10+0.08)
        // = 1.04*0.30 + 0.70 = 0.312 + 0.70 = 1.012
        MarketIndexResponse resp = service.createSnapshot(new MarketIndexCreateRequest(
                null, new BigDecimal("65.00"), new BigDecimal("410.00"),
                new BigDecimal("5800.00"), new BigDecimal("1850.00"),
                new BigDecimal("38.00"), new BigDecimal("92.00"), new BigDecimal("285.00"),
                STANDARD_WEIGHTS));
        assertThat(resp.compositeIndex()).isEqualByComparingTo("1.0120");
    }

    @Test
    void create_secondSnapshot_deactivatesPrevious() {
        MarketIndexResponse first = service.createSnapshot(new MarketIndexCreateRequest(
                null, new BigDecimal("62.50"), new BigDecimal("410.00"),
                new BigDecimal("5800.00"), new BigDecimal("1850.00"),
                new BigDecimal("38.00"), new BigDecimal("92.00"), new BigDecimal("285.00"),
                STANDARD_WEIGHTS));
        em.flush();

        service.createSnapshot(new MarketIndexCreateRequest(
                null, new BigDecimal("65.00"), new BigDecimal("410.00"),
                new BigDecimal("5800.00"), new BigDecimal("1850.00"),
                new BigDecimal("38.00"), new BigDecimal("92.00"), new BigDecimal("285.00"),
                STANDARD_WEIGHTS));
        em.flush();
        em.clear();

        MarketIndexSnapshot reloadedFirst = em.find(MarketIndexSnapshot.class, first.id());
        assertThat(reloadedFirst.isActive()).isFalse();
    }

    @Test
    void create_weightsBelow_0_99_throwsIllegalArgument() {
        Map<String, String> badWeights = Map.of("steel", "0.30", "cement", "0.20");  // sums to 0.50
        MarketIndexCreateRequest req = new MarketIndexCreateRequest(
                null, new BigDecimal("62.50"), new BigDecimal("410.00"),
                new BigDecimal("5800.00"), new BigDecimal("1850.00"),
                new BigDecimal("38.00"), new BigDecimal("92.00"), new BigDecimal("285.00"),
                badWeights);
        assertThatThrownBy(() -> service.createSnapshot(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weights");
    }

    @Test
    void getActive_returnsTheActiveOne() {
        service.createSnapshot(new MarketIndexCreateRequest(
                null, new BigDecimal("62.50"), new BigDecimal("410.00"),
                new BigDecimal("5800.00"), new BigDecimal("1850.00"),
                new BigDecimal("38.00"), new BigDecimal("92.00"), new BigDecimal("285.00"),
                STANDARD_WEIGHTS));
        em.flush();

        Optional<MarketIndexResponse> active = service.getActive();
        assertThat(active).isPresent();
        assertThat(active.get().active()).isTrue();
    }
}
