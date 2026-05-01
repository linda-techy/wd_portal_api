package com.wd.api.estimation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.estimation.domain.CustomisationCategory;
import com.wd.api.estimation.domain.CustomisationOption;
import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.domain.MarketIndexSnapshot;
import com.wd.api.estimation.domain.PackageRateVersion;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.PricingMode;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.dto.CalculatePreviewRequest;
import com.wd.api.estimation.dto.CustomisationChoiceDto;
import com.wd.api.estimation.dto.DimensionsDto;
import com.wd.api.estimation.dto.FloorDto;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class EstimationPreviewControllerIT extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private EntityManager em;

    private UUID packageId;

    @BeforeEach
    void seedMinimum() {
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

        packageId = pkg.getId();
    }

    @Test
    @WithMockUser
    void happyPath_handCalculatedSmoke_returns2911650() throws Exception {
        CalculatePreviewRequest req = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD, packageId, null, null,
                new DimensionsDto(
                        List.of(new FloorDto("GF", new BigDecimal("35"), new BigDecimal("30"))),
                        BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));

        mvc.perform(post("/api/estimations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chargeableArea").value(1050))
                .andExpect(jsonPath("$.baseCost").value(2467500.00))
                .andExpect(jsonPath("$.gst").value(444150.00))
                .andExpect(jsonPath("$.grandTotal").value(2911650.00));
    }

    @Test
    @WithMockUser
    void renovation_returns422() throws Exception {
        CalculatePreviewRequest req = new CalculatePreviewRequest(
                ProjectType.RENOVATION, packageId, null, null,
                new DimensionsDto(
                        List.of(new FloorDto("GF", new BigDecimal("35"), new BigDecimal("30"))),
                        BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));

        mvc.perform(post("/api/estimations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("unsupported-project-type"))
                .andExpect(jsonPath("$.projectType").value("RENOVATION"));
    }

    @Test
    @WithMockUser
    void unknownPackageId_returns400() throws Exception {
        CalculatePreviewRequest req = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD, UUID.randomUUID(), null, null,
                new DimensionsDto(
                        List.of(new FloorDto("GF", new BigDecimal("35"), new BigDecimal("30"))),
                        BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));

        mvc.perform(post("/api/estimations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid-request"));
    }

    @Test
    @WithMockUser
    void missingDimensions_returns400() throws Exception {
        // Manually-built JSON with `dimensions` field omitted
        String json = """
            {
              "projectType":"NEW_BUILD",
              "packageId":"%s",
              "customisations":[],
              "siteFees":[],
              "addOns":[],
              "govtFees":[],
              "discountPercent":0,
              "gstRate":0.18
            }
            """.formatted(packageId);

        mvc.perform(post("/api/estimations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void happyPath_withCustomisation_returnsLineItem() throws Exception {
        // Guards C1 fix: ensure the service correctly looks up the option's category and
        // passes the PricingMode through to the calculator. Before the fix, ANY non-empty
        // customisations list caused an NPE at line-item generation.
        CustomisationCategory category = new CustomisationCategory();
        category.setName("Flooring (test)");
        category.setPricingMode(PricingMode.PER_SQFT);
        em.persist(category);

        CustomisationOption option = new CustomisationOption();
        option.setCategoryId(category.getId());
        option.setName("Italian Marble (test)");
        option.setRate(new BigDecimal("950.00"));
        em.persist(option);
        em.flush();

        CalculatePreviewRequest req = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD, packageId, null, null,
                new DimensionsDto(
                        List.of(new FloorDto("GF", new BigDecimal("35"), new BigDecimal("30"))),
                        BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(new CustomisationChoiceDto(category.getId(), option.getId())),
                List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));

        mvc.perform(post("/api/estimations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                // customisation cost = 950.00 × 1050 sqft = 997,500 (using simplified PR-3 delta semantics)
                .andExpect(jsonPath("$.customisationCost").value(997500.00))
                // line items include BASE + CUSTOMISATION + GST (no discount, no fluctuation)
                .andExpect(jsonPath("$.lineItems[?(@.lineType == 'CUSTOMISATION')]").exists());
    }

    @Test
    @WithMockUser
    void omittingDiscountAndGst_appliesDefaults() throws Exception {
        // Guards I4 fix: discountPercent (default 0.00) and gstRate (default 0.18) are optional.
        String json = """
            {
              "projectType":"NEW_BUILD",
              "packageId":"%s",
              "dimensions":{
                "floors":[{"floorName":"GF","length":35,"width":30}],
                "semiCoveredArea":0,
                "openTerraceArea":0
              },
              "customisations":[],
              "siteFees":[],
              "addOns":[],
              "govtFees":[]
            }
            """.formatted(packageId);

        mvc.perform(post("/api/estimations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discount").value(0))
                // GST = 2,467,500 × 0.18 = 444,150 (default applied)
                .andExpect(jsonPath("$.gst").value(444150.00))
                .andExpect(jsonPath("$.grandTotal").value(2911650.00));
    }

    @Test
    void unauthenticated_returns401or403() throws Exception {
        // No @WithMockUser — should be rejected by Spring Security filter chain
        CalculatePreviewRequest req = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD, packageId, null, null,
                new DimensionsDto(
                        List.of(new FloorDto("GF", new BigDecimal("35"), new BigDecimal("30"))),
                        BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));

        mvc.perform(post("/api/estimations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                // Spring Security typically returns 401 for missing creds or 403 for missing authority — either is correct
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403, got " + status);
                    }
                });
    }
}
