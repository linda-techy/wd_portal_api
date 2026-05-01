package com.wd.api.estimation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.domain.MarketIndexSnapshot;
import com.wd.api.estimation.domain.PackageRateVersion;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.dto.CalculatePreviewRequest;
import com.wd.api.estimation.dto.DimensionsDto;
import com.wd.api.estimation.dto.FloorDto;
import com.wd.api.estimation.dto.admin.RateVersionCreateRequest;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class SettingsAdminPreviewIntegrationIT extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private EntityManager em;

    @Test
    @WithMockUser(authorities = {"ESTIMATION_SETTINGS_MANAGE", "ESTIMATION_SETTINGS_VIEW"})
    void newRateVersion_immediatelyReflectsInPreview() throws Exception {
        // Seed: package + initial rate version + market index
        EstimationPackage pkg = persistPackage();
        persistRateVersion(pkg, "1500.00", LocalDate.of(2026, 4, 1), null);
        persistActiveMarketIndex();
        em.flush();

        // Capture initial baseCost via preview (1050 sqft × 2350 = 2,467,500)
        MvcResult r1 = mvc.perform(post("/api/estimations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(previewRequest(pkg.getId()))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode b1 = mapper.readTree(r1.getResponse().getContentAsString());
        assertThat(b1.get("baseCost").decimalValue()).isEqualByComparingTo("2467500.00");

        // Admin publishes a new rate version with material rate up by 100/sqft
        // New baseRate = 1600 + 550 + 300 = 2450 → baseCost = 1050 × 2450 = 2,572,500
        RateVersionCreateRequest rvReq = new RateVersionCreateRequest(
                pkg.getId(), ProjectType.NEW_BUILD,
                new BigDecimal("1600.00"), new BigDecimal("550.00"), new BigDecimal("300.00"),
                null);
        mvc.perform(post("/api/estimation/rate-versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(rvReq)))
                .andExpect(status().isCreated());

        em.flush();
        em.clear();

        // Preview again — should now use the new rate
        MvcResult r2 = mvc.perform(post("/api/estimations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(previewRequest(pkg.getId()))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode b2 = mapper.readTree(r2.getResponse().getContentAsString());
        assertThat(b2.get("baseCost").decimalValue())
                .as("After admin publishes a new rate version, preview must use the new rates")
                .isEqualByComparingTo("2572500.00");
    }

    private CalculatePreviewRequest previewRequest(UUID packageId) {
        return new CalculatePreviewRequest(
                ProjectType.NEW_BUILD, packageId, null, null,
                new DimensionsDto(
                        List.of(new FloorDto("GF", new BigDecimal("35"), new BigDecimal("30"))),
                        BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));
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

    private void persistActiveMarketIndex() {
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
    }
}
