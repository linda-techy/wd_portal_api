package com.wd.api.estimation.persistence;

import com.wd.api.estimation.domain.Estimation;
import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.domain.MarketIndexSnapshot;
import com.wd.api.estimation.domain.PackageRateVersion;
import com.wd.api.estimation.domain.enums.EstimationStatus;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.model.Lead;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstimationImmutabilityTriggerTest extends TestcontainersPostgresBase {

    @Autowired
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    private static boolean triggerInstalled = false;

    @BeforeEach
    void installTriggerOnce() {
        if (triggerInstalled) return;
        // Install the V100 trigger by hand because tests use Hibernate create-drop, not Flyway.
        jdbc.execute("""
            CREATE OR REPLACE FUNCTION enforce_estimation_accepted_immutability()
            RETURNS TRIGGER AS $$
            BEGIN
                IF OLD.status = 'ACCEPTED' AND (
                    NEW.lead_id          IS DISTINCT FROM OLD.lead_id          OR
                    NEW.package_id       IS DISTINCT FROM OLD.package_id       OR
                    NEW.estimation_no    IS DISTINCT FROM OLD.estimation_no    OR
                    NEW.rate_version_id  IS DISTINCT FROM OLD.rate_version_id  OR
                    NEW.market_index_id  IS DISTINCT FROM OLD.market_index_id  OR
                    NEW.dimensions_json  IS DISTINCT FROM OLD.dimensions_json  OR
                    NEW.grand_total      IS DISTINCT FROM OLD.grand_total
                ) THEN
                    RAISE EXCEPTION 'Accepted estimations are immutable';
                END IF;
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql;
            """);
        jdbc.execute("DROP TRIGGER IF EXISTS trg_estimation_immutable ON estimation");
        jdbc.execute("""
            CREATE TRIGGER trg_estimation_immutable
                BEFORE UPDATE ON estimation
                FOR EACH ROW EXECUTE FUNCTION enforce_estimation_accepted_immutability();
            """);
        triggerInstalled = true;
    }

    private UUID createAcceptedEstimation() {
        Lead lead = new Lead();
        lead.setName("Accepted Customer");
        em.persist(lead);

        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("Signature");
        em.persist(pkg);

        PackageRateVersion rv = new PackageRateVersion();
        rv.setPackageId(pkg.getId());
        rv.setProjectType(ProjectType.NEW_BUILD);
        rv.setMaterialRate(new BigDecimal("1500.00"));
        rv.setLabourRate(new BigDecimal("550.00"));
        rv.setOverheadRate(new BigDecimal("300.00"));
        rv.setEffectiveFrom(LocalDate.of(2026, 4, 1));
        em.persist(rv);

        MarketIndexSnapshot mi = new MarketIndexSnapshot();
        mi.setSnapshotDate(LocalDate.of(2026, 4, 30));
        mi.setSteelRate(new BigDecimal("62.50"));
        mi.setCementRate(new BigDecimal("410.00"));
        mi.setSandRate(new BigDecimal("5800.00"));
        mi.setAggregateRate(new BigDecimal("1850.00"));
        mi.setTilesRate(new BigDecimal("38.00"));
        mi.setElectricalRate(new BigDecimal("92.00"));
        mi.setPaintsRate(new BigDecimal("285.00"));
        mi.setWeightsJson(Map.<String, Object>of("steel", "0.30"));
        mi.setCompositeIndex(new BigDecimal("1.0000"));
        em.persist(mi);

        Estimation est = new Estimation();
        est.setEstimationNo("EST-IMMUT-" + UUID.randomUUID());
        est.setLeadId(lead.getId());
        est.setProjectType(ProjectType.NEW_BUILD);
        est.setPackageId(pkg.getId());
        est.setRateVersionId(rv.getId());
        est.setMarketIndexId(mi.getId());
        est.setDimensionsJson(Map.<String, Object>of("floors", "[]"));
        est.setStatus(EstimationStatus.ACCEPTED);
        est.setGrandTotal(new BigDecimal("1000.00"));
        em.persist(est);
        em.flush();
        return est.getId();
    }

    @Test
    @Transactional
    void blocks_grandTotal_change_on_accepted() {
        UUID id = createAcceptedEstimation();
        em.flush();

        assertThatThrownBy(() -> {
            jdbc.update("UPDATE estimation SET grand_total = 9999 WHERE id = ?::uuid", id.toString());
        }).hasMessageContaining("Accepted estimations are immutable");
    }

    @Test
    @Transactional
    void blocks_dimensionsJson_change_on_accepted() {
        UUID id = createAcceptedEstimation();
        em.flush();

        assertThatThrownBy(() -> {
            jdbc.update("UPDATE estimation SET dimensions_json = '{\"floors\":\"changed\"}'::jsonb WHERE id = ?::uuid", id.toString());
        }).hasMessageContaining("Accepted estimations are immutable");
    }

    @Test
    @Transactional
    void blocks_rateVersionId_change_on_accepted() {
        UUID id = createAcceptedEstimation();
        UUID otherRv = UUID.randomUUID();
        em.flush();

        assertThatThrownBy(() -> {
            jdbc.update("UPDATE estimation SET rate_version_id = ?::uuid WHERE id = ?::uuid",
                    otherRv.toString(), id.toString());
        }).hasMessageContaining("Accepted estimations are immutable");
    }

    @Test
    @Transactional
    void blocks_marketIndexId_change_on_accepted() {
        UUID id = createAcceptedEstimation();
        UUID otherMi = UUID.randomUUID();
        em.flush();

        assertThatThrownBy(() -> {
            jdbc.update("UPDATE estimation SET market_index_id = ?::uuid WHERE id = ?::uuid",
                    otherMi.toString(), id.toString());
        }).hasMessageContaining("Accepted estimations are immutable");
    }

    @Test
    @Transactional
    void allows_status_change_on_accepted() {
        // The trigger only blocks the listed fields; status changes (e.g., REJECTED after ACCEPTED, for refund flows) are allowed.
        UUID id = createAcceptedEstimation();
        em.flush();
        int updated = jdbc.update("UPDATE estimation SET status = 'REJECTED' WHERE id = ?::uuid", id.toString());
        assertThat(updated).isEqualTo(1);
    }
}
