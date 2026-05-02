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
import com.wd.api.estimation.dto.LeadEstimationCreateRequest;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class LeadEstimationControllerIT extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private EntityManager em;

    private UUID packageId;

    @BeforeEach
    void seed() {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("Signature");
        pkg.setDisplayOrder(20);
        em.persist(pkg);
        packageId = pkg.getId();

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
    }

    private LeadEstimationCreateRequest buildCreateRequest() {
        DimensionsDto dim = new DimensionsDto(
                List.of(new FloorDto("Ground", new BigDecimal("30"), new BigDecimal("35"))),
                BigDecimal.ZERO, BigDecimal.ZERO);
        CalculatePreviewRequest preview = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD, packageId, null, null, dim,
                List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));
        return new LeadEstimationCreateRequest(1L, preview, null);
    }

    // -----------------------------------------------------------------------
    // POST returns 201 for valid STANDARD/NEW_BUILD request
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"LEAD_CREATE"})
    void post_validRequest_returns201WithEstimation() throws Exception {
        MvcResult result = mvc.perform(post("/api/lead-estimations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("success").asBoolean()).isTrue();
        JsonNode data = body.get("data");
        assertThat(data.get("estimationNo").asText()).startsWith("EST-");
        assertThat(data.get("status").asText()).isEqualTo("DRAFT");
        assertThat(new BigDecimal(data.get("grandTotal").asText())).isPositive();
    }

    // -----------------------------------------------------------------------
    // GET list returns the created estimation
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"LEAD_CREATE", "LEAD_VIEW"})
    void get_list_returnsCreatedEstimation() throws Exception {
        // Create one estimation
        mvc.perform(post("/api/lead-estimations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated());

        em.flush();
        em.clear();

        // List for leadId=1
        MvcResult result = mvc.perform(get("/api/lead-estimations")
                        .param("leadId", "1"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("success").asBoolean()).isTrue();
        JsonNode data = body.get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(1);
        assertThat(data.get(0).get("estimationNo").asText()).startsWith("EST-");
    }

    // -----------------------------------------------------------------------
    // GET single returns full detail with line items
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"LEAD_CREATE", "LEAD_VIEW"})
    void get_single_returnsDetailWithLineItems() throws Exception {
        // Create
        MvcResult createResult = mvc.perform(post("/api/lead-estimations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = mapper.readTree(createResult.getResponse().getContentAsString());
        String id = created.get("data").get("id").asText();

        em.flush();
        em.clear();

        // Get by ID
        MvcResult result = mvc.perform(get("/api/lead-estimations/{id}", id))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("success").asBoolean()).isTrue();
        JsonNode data = body.get("data");
        assertThat(data.get("id").asText()).isEqualTo(id);
        assertThat(data.get("lineItems").isArray()).isTrue();
        assertThat(data.get("lineItems").size()).isGreaterThan(0);
    }

    // -----------------------------------------------------------------------
    // DELETE returns 200, then GET single returns 404 (IllegalArgumentException → 400)
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"LEAD_CREATE", "LEAD_VIEW", "LEAD_DELETE"})
    void delete_returns200_thenGetReturns4xx() throws Exception {
        // Create
        MvcResult createResult = mvc.perform(post("/api/lead-estimations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = mapper.readTree(createResult.getResponse().getContentAsString());
        String id = created.get("data").get("id").asText();

        em.flush();

        // Delete
        mvc.perform(delete("/api/lead-estimations/{id}", id))
                .andExpect(status().isOk());

        em.flush();
        em.clear();

        // Subsequent GET returns 400 (IllegalArgumentException "Estimation not found")
        // GlobalExceptionHandler maps IllegalArgumentException → 400 Bad Request
        mvc.perform(get("/api/lead-estimations/{id}", id))
                .andExpect(status().is4xxClientError());
    }

    // -----------------------------------------------------------------------
    // POST without LEAD_CREATE returns 403
    // -----------------------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"LEAD_VIEW"})
    void post_withoutCreatePermission_returns403() throws Exception {
        mvc.perform(post("/api/lead-estimations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isForbidden());
    }
}
