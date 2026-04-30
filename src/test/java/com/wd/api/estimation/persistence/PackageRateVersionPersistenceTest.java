package com.wd.api.estimation.persistence;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PackageRateVersionPersistenceTest extends TestcontainersPostgresBase {

    @Autowired
    private EntityManager em;

    @Test
    void roundTrip_packageRateVersion() {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("Signature");
        em.persist(pkg);
        em.flush();

        PackageRateVersion rv = new PackageRateVersion();
        rv.setPackageId(pkg.getId());
        rv.setProjectType(ProjectType.NEW_BUILD);
        rv.setMaterialRate(new BigDecimal("1500.00"));
        rv.setLabourRate(new BigDecimal("550.00"));
        rv.setOverheadRate(new BigDecimal("300.00"));
        rv.setEffectiveFrom(LocalDate.of(2026, 4, 1));

        em.persist(rv);
        em.flush();
        UUID id = rv.getId();
        em.clear();

        PackageRateVersion loaded = em.find(PackageRateVersion.class, id);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getPackageId()).isEqualTo(pkg.getId());
        assertThat(loaded.getProjectType()).isEqualTo(ProjectType.NEW_BUILD);
        assertThat(loaded.getMaterialRate()).isEqualByComparingTo("1500.00");
        assertThat(loaded.getLabourRate()).isEqualByComparingTo("550.00");
        assertThat(loaded.getOverheadRate()).isEqualByComparingTo("300.00");
        assertThat(loaded.getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(loaded.getEffectiveTo()).isNull();
    }
}
