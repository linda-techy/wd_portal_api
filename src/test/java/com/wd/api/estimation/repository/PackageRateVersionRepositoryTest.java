package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.domain.PackageRateVersion;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PackageRateVersionRepositoryTest extends TestcontainersPostgresBase {

    @Autowired private EntityManager em;
    @Autowired private PackageRateVersionRepository repo;

    @Test
    void findActive_returnsCurrentVersion() {
        EstimationPackage pkg = persistPackage();
        PackageRateVersion rv = persistRv(pkg, "1500.00", LocalDate.of(2026, 4, 1), null);

        Optional<PackageRateVersion> found = repo.findActive(
                pkg.getId(), ProjectType.NEW_BUILD, LocalDate.of(2026, 4, 30));

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(rv.getId());
    }

    @Test
    void findActive_returnsNothing_whenAsOfIsBeforeEffectiveFrom() {
        EstimationPackage pkg = persistPackage();
        persistRv(pkg, "1500.00", LocalDate.of(2026, 4, 1), null);

        Optional<PackageRateVersion> found = repo.findActive(
                pkg.getId(), ProjectType.NEW_BUILD, LocalDate.of(2026, 3, 31));

        assertThat(found).isEmpty();
    }

    @Test
    void findActive_returnsNothing_whenAsOfIsAfterEffectiveTo() {
        EstimationPackage pkg = persistPackage();
        persistRv(pkg, "1500.00",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31));   // closed window

        Optional<PackageRateVersion> found = repo.findActive(
                pkg.getId(), ProjectType.NEW_BUILD, LocalDate.of(2026, 4, 30));

        assertThat(found).isEmpty();
    }

    @Test
    void findActive_returnsLatestWhenMultipleActive() {
        EstimationPackage pkg = persistPackage();
        PackageRateVersion older = persistRv(pkg, "1400.00", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1));
        PackageRateVersion newer = persistRv(pkg, "1500.00", LocalDate.of(2026, 4, 1), null);

        Optional<PackageRateVersion> found = repo.findActive(
                pkg.getId(), ProjectType.NEW_BUILD, LocalDate.of(2026, 4, 30));

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(newer.getId());
        assertThat(found.get().getMaterialRate()).isEqualByComparingTo("1500.00");
    }

    private EstimationPackage persistPackage() {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("Signature");
        em.persist(pkg);
        em.flush();
        return pkg;
    }

    private PackageRateVersion persistRv(EstimationPackage pkg, String matRate,
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
        em.flush();
        return rv;
    }
}
