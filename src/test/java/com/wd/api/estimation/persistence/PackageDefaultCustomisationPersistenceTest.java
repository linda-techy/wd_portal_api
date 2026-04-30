package com.wd.api.estimation.persistence;

import com.wd.api.estimation.domain.*;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.PricingMode;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PackageDefaultCustomisationPersistenceTest extends TestcontainersPostgresBase {

    @Autowired
    private EntityManager em;

    @Test
    void roundTrip_packageDefaultCustomisation() {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("Signature");
        em.persist(pkg);

        CustomisationCategory cat = new CustomisationCategory();
        cat.setName("Flooring");
        cat.setPricingMode(PricingMode.PER_SQFT);
        em.persist(cat);

        CustomisationOption opt = new CustomisationOption();
        opt.setCategoryId(cat.getId());
        opt.setName("Vitrified Tiles");
        opt.setRate(new BigDecimal("180.00"));
        em.persist(opt);
        em.flush();

        PackageDefaultCustomisation def = new PackageDefaultCustomisation();
        def.setPackageId(pkg.getId());
        def.setCategoryId(cat.getId());
        def.setOptionId(opt.getId());

        em.persist(def);
        em.flush();
        UUID id = def.getId();
        em.clear();

        PackageDefaultCustomisation loaded = em.find(PackageDefaultCustomisation.class, id);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getPackageId()).isEqualTo(pkg.getId());
        assertThat(loaded.getCategoryId()).isEqualTo(cat.getId());
        assertThat(loaded.getOptionId()).isEqualTo(opt.getId());
    }
}
