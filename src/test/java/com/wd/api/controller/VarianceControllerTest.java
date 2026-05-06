package com.wd.api.controller;

import com.wd.api.service.scheduling.NoBaselineException;
import com.wd.api.service.scheduling.VarianceService;
import com.wd.api.service.scheduling.dto.VarianceRowDto;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class VarianceControllerTest extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @MockitoBean private VarianceService varianceService;

    @Test
    @WithMockUser(authorities = {"TASK_VIEW"})
    void get_variance_happyPath() throws Exception {
        when(varianceService.computeFor(42L)).thenReturn(List.of(
                new VarianceRowDto(101L, "Slab",
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10),
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 12),
                        null, null,
                        2, null, null, true)));
        mvc.perform(get("/api/projects/42/variance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].taskId").value(101))
                .andExpect(jsonPath("$.data[0].planVsBaselineDays").value(2))
                .andExpect(jsonPath("$.data[0].isCritical").value(true));
    }

    @Test
    @WithMockUser(authorities = {"TASK_VIEW"})
    void get_variance_noBaseline_returns404() throws Exception {
        when(varianceService.computeFor(42L)).thenThrow(new NoBaselineException(42L));
        mvc.perform(get("/api/projects/42/variance"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"PROJECT_BASELINE_APPROVE"})
    void get_variance_withoutTaskView_returns403() throws Exception {
        mvc.perform(get("/api/projects/42/variance"))
                .andExpect(status().isForbidden());
    }
}
