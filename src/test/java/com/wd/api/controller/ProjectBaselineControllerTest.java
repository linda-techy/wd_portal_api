package com.wd.api.controller;

import com.wd.api.service.scheduling.BaselineAlreadyExistsException;
import com.wd.api.service.scheduling.NoBaselineException;
import com.wd.api.service.scheduling.ProjectBaselineService;
import com.wd.api.service.scheduling.dto.ApproveBaselineResponse;
import com.wd.api.service.scheduling.dto.ProjectBaselineDto;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ProjectBaselineControllerTest extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @MockitoBean private ProjectBaselineService baselineService;

    @Test
    @WithMockUser(authorities = {"PROJECT_BASELINE_APPROVE"})
    void post_approve_happyPath() throws Exception {
        when(baselineService.approve(eq(42L), any())).thenReturn(
                new ApproveBaselineResponse(
                        99L, 42L,
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 1),
                        17));
        mvc.perform(post("/api/projects/42/baseline").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baselineId").value(99))
                .andExpect(jsonPath("$.data.taskCount").value(17));
    }

    @Test
    @WithMockUser(authorities = {"PROJECT_BASELINE_APPROVE"})
    void post_approve_secondTime_returns409() throws Exception {
        when(baselineService.approve(eq(42L), any()))
                .thenThrow(new BaselineAlreadyExistsException(42L));
        mvc.perform(post("/api/projects/42/baseline").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = {"TASK_VIEW"})
    void post_approve_withoutPermission_returns403() throws Exception {
        mvc.perform(post("/api/projects/42/baseline").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"TASK_VIEW"})
    void get_baseline_happyPath() throws Exception {
        when(baselineService.getBaseline(42L)).thenReturn(
                new ProjectBaselineDto(99L, 42L, null, 7L,
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 1), List.of()));
        mvc.perform(get("/api/projects/42/baseline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(99));
    }

    @Test
    @WithMockUser(authorities = {"TASK_VIEW"})
    void get_baseline_whenAbsent_returns404() throws Exception {
        when(baselineService.getBaseline(42L)).thenThrow(new NoBaselineException(42L));
        mvc.perform(get("/api/projects/42/baseline"))
                .andExpect(status().isNotFound());
    }
}
