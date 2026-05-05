package com.wd.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wd.api.dto.scheduling.HolidayOverrideRequest;
import com.wd.api.dto.scheduling.ProjectScheduleConfigDto;
import com.wd.api.model.scheduling.HolidayOverrideAction;
import com.wd.api.model.scheduling.ProjectHolidayOverride;
import com.wd.api.repository.ProjectHolidayOverrideRepository;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class ProjectScheduleConfigControllerTest extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private ProjectScheduleConfigRepository configRepo;
    @Autowired private ProjectHolidayOverrideRepository overrideRepo;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void clean() {
        overrideRepo.deleteAll();
        configRepo.deleteAll();
    }

    @Test
    void unauthenticated_get_returns401() throws Exception {
        mvc.perform(get("/api/projects/42/schedule-config"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "HOLIDAY_VIEW")
    void get_returns200_andDefaults() throws Exception {
        mvc.perform(get("/api/projects/42/schedule-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(42))
                .andExpect(jsonPath("$.sundayWorking").value(false));
    }

    @Test
    @WithMockUser(authorities = "HOLIDAY_VIEW")
    void put_withoutEditAuthority_returns403() throws Exception {
        ProjectScheduleConfigDto dto = new ProjectScheduleConfigDto(
                42L, true, (short) 601, (short) 930, "KL-EKM");
        mvc.perform(put("/api/projects/42/schedule-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PROJECT_SCHEDULE_CONFIG_EDIT")
    void put_withEditAuthority_returns200() throws Exception {
        ProjectScheduleConfigDto dto = new ProjectScheduleConfigDto(
                42L, true, (short) 601, (short) 930, "KL-EKM");
        mvc.perform(put("/api/projects/42/schedule-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.districtCode").value("KL-EKM"));
    }

    @Test
    @WithMockUser(authorities = "PROJECT_HOLIDAY_OVERRIDE")
    void postOverride_returns201() throws Exception {
        HolidayOverrideRequest req = new HolidayOverrideRequest(
                HolidayOverrideAction.ADD, LocalDate.of(2026, 12, 31), null, "Office shutdown");
        mvc.perform(post("/api/projects/42/holiday-overrides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "PROJECT_HOLIDAY_OVERRIDE")
    void postOverride_invalidPayload_returns400() throws Exception {
        // Missing required action and overrideDate
        mvc.perform(post("/api/projects/42/holiday-overrides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "PROJECT_HOLIDAY_OVERRIDE")
    void deleteOverride_returns204() throws Exception {
        // Seed an override row so the service finds something to delete.
        ProjectHolidayOverride o = new ProjectHolidayOverride();
        o.setProjectId(42L);
        o.setOverrideDate(LocalDate.of(2026, 12, 31));
        o.setOverrideName("Test");
        o.setAction(HolidayOverrideAction.ADD);
        Long id = overrideRepo.save(o).getId();

        mvc.perform(delete("/api/projects/42/holiday-overrides/" + id))
                .andExpect(status().isNoContent());
    }
}
