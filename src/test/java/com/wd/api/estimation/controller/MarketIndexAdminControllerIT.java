package com.wd.api.estimation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.estimation.dto.admin.MarketIndexCreateRequest;
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
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class MarketIndexAdminControllerIT extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private EntityManager em;

    private static final Map<String, String> STANDARD_WEIGHTS = Map.of(
            "steel", "0.30", "cement", "0.20", "sand", "0.12",
            "aggregate", "0.08", "tiles", "0.12", "electrical", "0.10", "paints", "0.08");

    @Test
    @WithMockUser(authorities = "ESTIMATION_MARKET_INDEX_PUBLISH")
    void publish_firstSnapshot_returns201_compositeIs1_0000() throws Exception {
        MarketIndexCreateRequest req = new MarketIndexCreateRequest(
                null, new BigDecimal("62.50"), new BigDecimal("410.00"),
                new BigDecimal("5800.00"), new BigDecimal("1850.00"),
                new BigDecimal("38.00"), new BigDecimal("92.00"), new BigDecimal("285.00"),
                STANDARD_WEIGHTS);

        mvc.perform(post("/api/estimation/market-index")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.compositeIndex").value(1.0000))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @WithMockUser(authorities = "ESTIMATION_MARKET_INDEX_PUBLISH")
    void publish_withWeightsSummingTo_0_50_returns400() throws Exception {
        Map<String, String> badWeights = Map.of("steel", "0.30", "cement", "0.20");
        MarketIndexCreateRequest req = new MarketIndexCreateRequest(
                null, new BigDecimal("62.50"), new BigDecimal("410.00"),
                new BigDecimal("5800.00"), new BigDecimal("1850.00"),
                new BigDecimal("38.00"), new BigDecimal("92.00"), new BigDecimal("285.00"),
                badWeights);

        mvc.perform(post("/api/estimation/market-index")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ESTIMATION_SETTINGS_VIEW")
    void publish_withOnlyViewPermission_returns403() throws Exception {
        MarketIndexCreateRequest req = new MarketIndexCreateRequest(
                null, new BigDecimal("62.50"), new BigDecimal("410.00"),
                new BigDecimal("5800.00"), new BigDecimal("1850.00"),
                new BigDecimal("38.00"), new BigDecimal("92.00"), new BigDecimal("285.00"),
                STANDARD_WEIGHTS);

        mvc.perform(post("/api/estimation/market-index")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ESTIMATION_SETTINGS_VIEW")
    void getActive_returns404_whenNoneActive() throws Exception {
        mvc.perform(get("/api/estimation/market-index/active"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(authorities = "ESTIMATION_SETTINGS_VIEW")
    void list_returns200_evenWhenEmpty() throws Exception {
        mvc.perform(get("/api/estimation/market-index"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
