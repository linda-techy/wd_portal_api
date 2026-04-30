package com.wd.api.estimation.persistence;

import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class EstimationPackagePersistenceTest extends TestcontainersPostgresBase {

    @Autowired
    private EntityManager em;

    @Test
    void roundTrip_estimationPackage() {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("Signature");
        pkg.setTagline("Mid-segment, branded materials");
        pkg.setDescription("Asian Paints, Jaquar mid, vitrified tiles. Most popular tier.");
        pkg.setDisplayOrder(20);
        pkg.setActive(true);

        em.persist(pkg);
        em.flush();
        UUID id = pkg.getId();
        assertThat(id).isNotNull();

        em.clear();

        EstimationPackage loaded = em.find(EstimationPackage.class, id);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getInternalName()).isEqualTo(PackageInternalName.STANDARD);
        assertThat(loaded.getMarketingName()).isEqualTo("Signature");
        assertThat(loaded.getTagline()).isEqualTo("Mid-segment, branded materials");
        assertThat(loaded.getDescription()).contains("Asian Paints");
        assertThat(loaded.getDisplayOrder()).isEqualTo(20);
        assertThat(loaded.isActive()).isTrue();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
        assertThat(loaded.getVersion()).isEqualTo(1L);
    }
}
