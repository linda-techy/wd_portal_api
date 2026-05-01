package com.wd.api.estimation.service.admin;

import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.domain.PackageRateVersion;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.dto.admin.RateVersionCreateRequest;
import com.wd.api.estimation.dto.admin.RateVersionResponse;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class PackageRateVersionAdminServiceTest extends TestcontainersPostgresBase {

    @Autowired private EntityManager em;
    @Autowired private PackageRateVersionAdminService service;

    @Test
    void create_firstVersion_setsEffectiveFromToToday_andEffectiveToNull() {
        EstimationPackage pkg = persistPackage();
        em.flush();

        RateVersionCreateRequest req = new RateVersionCreateRequest(
                pkg.getId(), ProjectType.NEW_BUILD,
                new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00"),
                null);
        RateVersionResponse resp = service.createNewVersion(req);

        assertThat(resp.effectiveFrom()).isEqualTo(LocalDate.now());
        assertThat(resp.effectiveTo()).isNull();
        assertThat(resp.materialRate()).isEqualByComparingTo("1500.00");
    }

    @Test
    void create_secondVersion_atomicallyClosesPreviousActive() {
        EstimationPackage pkg = persistPackage();
        PackageRateVersion oldVersion = persistRateVersion(pkg, "1500.00", LocalDate.of(2026, 4, 1), null);
        em.flush();

        RateVersionCreateRequest req = new RateVersionCreateRequest(
                pkg.getId(), ProjectType.NEW_BUILD,
                new BigDecimal("1600.00"), new BigDecimal("600.00"), new BigDecimal("350.00"),
                null);
        RateVersionResponse newResp = service.createNewVersion(req);
        em.flush();
        em.clear();

        // Old version's effective_to should now be today - 1
        PackageRateVersion reloadedOld = em.find(PackageRateVersion.class, oldVersion.getId());
        assertThat(reloadedOld.getEffectiveTo())
                .as("Previous active version should be closed atomically with the new insert")
                .isEqualTo(LocalDate.now().minusDays(1));

        // New version active
        PackageRateVersion reloadedNew = em.find(PackageRateVersion.class, newResp.id());
        assertThat(reloadedNew.getEffectiveTo()).isNull();
        assertThat(reloadedNew.getMaterialRate()).isEqualByComparingTo("1600.00");
    }

    @Test
    void create_withExplicitEffectiveFrom_usesThatDate() {
        EstimationPackage pkg = persistPackage();
        em.flush();

        RateVersionCreateRequest req = new RateVersionCreateRequest(
                pkg.getId(), ProjectType.NEW_BUILD,
                new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00"),
                LocalDate.of(2026, 6, 1));
        RateVersionResponse resp = service.createNewVersion(req);
        assertThat(resp.effectiveFrom()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    void create_unknownPackageId_throwsIllegalArgument() {
        RateVersionCreateRequest req = new RateVersionCreateRequest(
                UUID.randomUUID(), ProjectType.NEW_BUILD,
                new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00"),
                null);
        assertThatThrownBy(() -> service.createNewVersion(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("package");
    }

    @Test
    void list_filtersByPackageAndProjectType() {
        EstimationPackage pkg = persistPackage();
        persistRateVersion(pkg, "1500.00", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        persistRateVersion(pkg, "1600.00", LocalDate.of(2026, 5, 1), null);
        em.flush();

        List<RateVersionResponse> results = service.list(pkg.getId(), ProjectType.NEW_BUILD);
        assertThat(results).hasSize(2);
        assertThat(results).extracting(RateVersionResponse::materialRate)
                .containsExactlyInAnyOrder(new BigDecimal("1500.00"), new BigDecimal("1600.00"));
    }

    private EstimationPackage persistPackage() {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("Signature");
        pkg.setDisplayOrder(20);
        em.persist(pkg);
        return pkg;
    }

    private PackageRateVersion persistRateVersion(EstimationPackage pkg, String matRate,
                                                   LocalDate from, LocalDate to) {
        PackageRateVersion rv = new PackageRateVersion();
        rv.setPackageId(pkg.getId());
        rv.setProjectType(ProjectType.NEW_BUILD);
        rv.setMaterialRate(new BigDecimal(matRate));
        rv.setLabourRate(new BigDecimal("550.00"));
        rv.setOverheadRate(new BigDecimal("300.00"));
        rv.setEffectiveFrom(from);
        rv.setEffectiveTo(to);
        em.persist(rv);
        return rv;
    }
}
