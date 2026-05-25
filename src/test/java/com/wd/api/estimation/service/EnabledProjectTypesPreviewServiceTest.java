package com.wd.api.estimation.service;

import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.domain.MarketIndexSnapshot;
import com.wd.api.estimation.domain.PackageRateVersion;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.dto.CalculatePreviewRequest;
import com.wd.api.estimation.dto.CalculatePreviewResponse;
import com.wd.api.estimation.dto.DimensionsDto;
import com.wd.api.estimation.dto.FloorDto;
import com.wd.api.estimation.service.calc.exception.UnsupportedProjectTypeException;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests that RENOVATION / INTERIOR / COMPOUND route through the parametric engine:
 * (a) with a configured per-type rate version — succeeds and returns a breakdown;
 * (b) without a configured rate version — yields IllegalStateException("No active rate version …");
 * (c) NEW_BUILD / COMMERCIAL regression — behaviour unchanged.
 *
 * RENOVATION/INTERIOR/COMPOUND use the area-based parametric model with
 * business-configured per-type rate versions. If a project's pricing is
 * item-rate/measured, configure rates accordingly or use a manual BoQ.
 */
@Transactional
class EnabledProjectTypesPreviewServiceTest extends TestcontainersPostgresBase {

    @Autowired private EntityManager em;
    @Autowired private EstimationPreviewService service;

    // -----------------------------------------------------------------------
    // (a) SUCCESS: extended type with a configured per-type rate version
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(value = ProjectType.class, names = {"RENOVATION", "INTERIOR", "COMPOUND"})
    void extendedType_withActiveRateVersion_returnsParametricBreakdown(ProjectType type) {
        Setup s = seed(type);
        CalculatePreviewRequest req = lineItemRequest(type, s.packageId);

        CalculatePreviewResponse resp = service.preview(req);

        assertThat(resp).isNotNull();
        assertThat(resp.chargeableArea()).isEqualByComparingTo("1050");
        assertThat(resp.baseCost()).isEqualByComparingTo("2467500.00");
        assertThat(resp.grandTotal()).isPositive();
        // Must not carry UnsupportedProjectTypeException at any layer
        assertThat(resp.lineItems()).isNotEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = ProjectType.class, names = {"RENOVATION", "INTERIOR", "COMPOUND"})
    void extendedType_withActiveRateVersion_doesNotThrowUnsupportedProjectType(ProjectType type) {
        Setup s = seed(type);
        CalculatePreviewRequest req = lineItemRequest(type, s.packageId);

        // Calling preview() must complete without UnsupportedProjectTypeException
        assertThat(service.preview(req)).isNotNull();
    }

    // -----------------------------------------------------------------------
    // (b) NO RATE VERSION: clear IllegalStateException (not UnsupportedProjectTypeException)
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(value = ProjectType.class, names = {"RENOVATION", "INTERIOR", "COMPOUND"})
    void extendedType_withoutRateVersion_throwsIllegalStateWithClearMessage(ProjectType type) {
        // Seed a package but NO rate version for this project type
        UUID packageId = seedPackageOnly();
        CalculatePreviewRequest req = lineItemRequest(type, packageId);

        assertThatThrownBy(() -> service.preview(req))
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(UnsupportedProjectTypeException.class)
                .hasMessageContaining("No active rate version")
                .hasMessageContaining(type.name());
    }

    // -----------------------------------------------------------------------
    // (c) REGRESSION: NEW_BUILD / COMMERCIAL unchanged
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(value = ProjectType.class, names = {"NEW_BUILD", "COMMERCIAL"})
    void existingTypes_regression_behaviourUnchanged(ProjectType type) {
        Setup s = seed(type);
        CalculatePreviewRequest req = lineItemRequest(type, s.packageId);

        CalculatePreviewResponse resp = service.preview(req);

        assertThat(resp.chargeableArea()).isEqualByComparingTo("1050");
        assertThat(resp.baseCost()).isEqualByComparingTo("2467500.00");
        // GST 18%: 2467500 × 1.18 = 2911650
        assertThat(resp.grandTotal()).isEqualByComparingTo("2911650.00");
    }

    @Test
    void newBuild_noRateVersion_stillThrowsIllegalState_notUnsupportedType() {
        UUID packageId = seedPackageOnly();
        CalculatePreviewRequest req = lineItemRequest(ProjectType.NEW_BUILD, packageId);

        assertThatThrownBy(() -> service.preview(req))
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(UnsupportedProjectTypeException.class)
                .hasMessageContaining("rate version");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Setup seed(ProjectType type) {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("Signature");
        em.persist(pkg);

        PackageRateVersion rv = new PackageRateVersion();
        rv.setPackageId(pkg.getId());
        rv.setProjectType(type);
        rv.setMaterialRate(new BigDecimal("1500.00"));
        rv.setLabourRate(new BigDecimal("550.00"));
        rv.setOverheadRate(new BigDecimal("300.00"));
        rv.setEffectiveFrom(LocalDate.of(2026, 4, 1));
        em.persist(rv);

        MarketIndexSnapshot mi = new MarketIndexSnapshot();
        mi.setSnapshotDate(LocalDate.now());
        mi.setSteelRate(new BigDecimal("62.50"));
        mi.setCementRate(new BigDecimal("410.00"));
        mi.setSandRate(new BigDecimal("5800.00"));
        mi.setAggregateRate(new BigDecimal("1850.00"));
        mi.setTilesRate(new BigDecimal("38.00"));
        mi.setElectricalRate(new BigDecimal("92.00"));
        mi.setPaintsRate(new BigDecimal("285.00"));
        mi.setWeightsJson(Map.of("steel", "0.30"));
        mi.setCompositeIndex(new BigDecimal("1.0000"));
        mi.setActive(true);
        em.persist(mi);
        em.flush();

        return new Setup(pkg.getId());
    }

    private UUID seedPackageOnly() {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("Signature");
        em.persist(pkg);

        // Ensure there is an active market index (required before rate version lookup)
        MarketIndexSnapshot mi = new MarketIndexSnapshot();
        mi.setSnapshotDate(LocalDate.now());
        mi.setSteelRate(new BigDecimal("62.50"));
        mi.setCementRate(new BigDecimal("410.00"));
        mi.setSandRate(new BigDecimal("5800.00"));
        mi.setAggregateRate(new BigDecimal("1850.00"));
        mi.setTilesRate(new BigDecimal("38.00"));
        mi.setElectricalRate(new BigDecimal("92.00"));
        mi.setPaintsRate(new BigDecimal("285.00"));
        mi.setWeightsJson(Map.of("steel", "0.30"));
        mi.setCompositeIndex(new BigDecimal("1.0000"));
        mi.setActive(true);
        em.persist(mi);
        em.flush();

        return pkg.getId();
    }

    private CalculatePreviewRequest lineItemRequest(ProjectType type, UUID packageId) {
        return new CalculatePreviewRequest(
                type,
                packageId,
                null, null,
                new DimensionsDto(
                        List.of(new FloorDto("GF", new BigDecimal("35"), new BigDecimal("30"))),
                        BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));
    }

    private record Setup(UUID packageId) {}
}
