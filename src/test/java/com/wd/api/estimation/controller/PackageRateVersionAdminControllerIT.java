package com.wd.api.estimation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.domain.PackageRateVersion;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.dto.admin.RateVersionCreateRequest;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class PackageRateVersionAdminControllerIT extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private EntityManager em;

    @Test
    @WithMockUser(authorities = "ESTIMATION_SETTINGS_MANAGE")
    void create_returns201() throws Exception {
        EstimationPackage pkg = seedPackage();
        em.flush();

        RateVersionCreateRequest req = new RateVersionCreateRequest(
                pkg.getId(), ProjectType.NEW_BUILD,
                new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00"),
                null);
        mvc.perform(post("/api/estimation/rate-versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.materialRate").value(1500.00))
                .andExpect(jsonPath("$.data.effectiveTo").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "ESTIMATION_SETTINGS_VIEW")
    void getActive_returns200_whenOneExists() throws Exception {
        EstimationPackage pkg = seedPackage();
        seedRv(pkg, LocalDate.of(2026, 4, 1), null);
        em.flush();

        mvc.perform(get("/api/estimation/rate-versions/active")
                        .param("packageId", pkg.getId().toString())
                        .param("projectType", "NEW_BUILD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.packageId").value(pkg.getId().toString()));
    }

    @Test
    @WithMockUser(authorities = "ESTIMATION_SETTINGS_VIEW")
    void getActive_returns404_whenNoneExists() throws Exception {
        EstimationPackage pkg = seedPackage();
        em.flush();

        mvc.perform(get("/api/estimation/rate-versions/active")
                        .param("packageId", pkg.getId().toString())
                        .param("projectType", "NEW_BUILD"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(authorities = "ESTIMATION_SETTINGS_VIEW")
    void list_returns200_filteredByPackageAndProjectType() throws Exception {
        EstimationPackage pkg = seedPackage();
        seedRv(pkg, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        seedRv(pkg, LocalDate.of(2026, 5, 1), null);
        em.flush();

        mvc.perform(get("/api/estimation/rate-versions")
                        .param("packageId", pkg.getId().toString())
                        .param("projectType", "NEW_BUILD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "ESTIMATION_SETTINGS_VIEW")
    void create_withOnlyViewPermission_returns403() throws Exception {
        EstimationPackage pkg = seedPackage();
        em.flush();

        RateVersionCreateRequest req = new RateVersionCreateRequest(
                pkg.getId(), ProjectType.NEW_BUILD,
                new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00"),
                null);
        mvc.perform(post("/api/estimation/rate-versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    private EstimationPackage seedPackage() {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("Signature");
        pkg.setDisplayOrder(20);
        em.persist(pkg);
        return pkg;
    }

    private PackageRateVersion seedRv(EstimationPackage pkg, LocalDate from, LocalDate to) {
        PackageRateVersion rv = new PackageRateVersion();
        rv.setPackageId(pkg.getId());
        rv.setProjectType(ProjectType.NEW_BUILD);
        rv.setMaterialRate(new BigDecimal("1500.00"));
        rv.setLabourRate(new BigDecimal("550.00"));
        rv.setOverheadRate(new BigDecimal("300.00"));
        rv.setEffectiveFrom(from);
        rv.setEffectiveTo(to);
        em.persist(rv);
        return rv;
    }
}
