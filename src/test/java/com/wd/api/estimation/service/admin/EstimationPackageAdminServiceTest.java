package com.wd.api.estimation.service.admin;

import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.dto.admin.PackageAdminCreateRequest;
import com.wd.api.estimation.dto.admin.PackageAdminResponse;
import com.wd.api.estimation.dto.admin.PackageAdminUpdateRequest;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class EstimationPackageAdminServiceTest extends TestcontainersPostgresBase {

    @Autowired private EntityManager em;
    @Autowired private EstimationPackageAdminService service;

    @Test
    void create_returnsResponseWithGeneratedId() {
        PackageAdminCreateRequest req = new PackageAdminCreateRequest(
                PackageInternalName.STANDARD, "Signature", "Mid-segment", "desc", 20);
        PackageAdminResponse resp = service.create(req);
        assertThat(resp.id()).isNotNull();
        assertThat(resp.internalName()).isEqualTo(PackageInternalName.STANDARD);
        assertThat(resp.marketingName()).isEqualTo("Signature");
        assertThat(resp.active()).isTrue();
    }

    @Test
    void update_persistsAllMutableFields() {
        EstimationPackage pkg = persistPackage(PackageInternalName.STANDARD, "Old Name", 20);
        em.flush();

        PackageAdminUpdateRequest req = new PackageAdminUpdateRequest(
                "New Name", "New tagline", "New desc", 25, false);
        PackageAdminResponse resp = service.update(pkg.getId(), req);

        assertThat(resp.marketingName()).isEqualTo("New Name");
        assertThat(resp.tagline()).isEqualTo("New tagline");
        assertThat(resp.displayOrder()).isEqualTo(25);
        assertThat(resp.active()).isFalse();
    }

    @Test
    void update_unknownId_throwsIllegalArgument() {
        PackageAdminUpdateRequest req = new PackageAdminUpdateRequest(
                "Doesn't matter", null, null, 10, true);
        assertThatThrownBy(() -> service.update(UUID.randomUUID(), req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void list_returnsActivePackagesByDisplayOrder() {
        persistPackage(PackageInternalName.STANDARD, "Mid", 20);
        persistPackage(PackageInternalName.BASIC,    "Cheap", 10);
        persistPackage(PackageInternalName.PREMIUM,  "Pricey", 30);
        em.flush();

        List<PackageAdminResponse> all = service.list(false);
        assertThat(all).extracting(PackageAdminResponse::displayOrder)
                .containsExactly(10, 20, 30);
    }

    @Test
    void delete_marksRowAsSoftDeleted_andHidesFromActiveList() {
        EstimationPackage pkg = persistPackage(PackageInternalName.STANDARD, "Signature", 20);
        em.flush();

        service.softDelete(pkg.getId());
        em.flush();
        em.clear();

        // Default active list excludes soft-deleted (the @Where clause on the entity)
        assertThat(service.list(false)).extracting(PackageAdminResponse::id)
                .doesNotContain(pkg.getId());
    }

    private EstimationPackage persistPackage(PackageInternalName name, String marketing, int order) {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(name);
        pkg.setMarketingName(marketing);
        pkg.setDisplayOrder(order);
        em.persist(pkg);
        return pkg;
    }
}
