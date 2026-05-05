package com.wd.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wd.api.dto.scheduling.HolidayDto;
import com.wd.api.model.scheduling.Holiday;
import com.wd.api.model.scheduling.HolidayRecurrenceType;
import com.wd.api.model.scheduling.HolidayScope;
import com.wd.api.repository.HolidayRepository;
import com.wd.api.service.scheduling.HolidayService;
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

/**
 * Integration test for HolidayAdminController. Uses @AutoConfigureMockMvc on
 * the full Spring context (TestcontainersPostgresBase) — the codebase's
 * @WebMvcTest path is blocked by WebMvcConfig requiring RateLimiterConfig
 * which is excluded by @WebMvcTest's slice. Existing controller tests
 * (estimation/...IT.java) use this same pattern.
 */
@AutoConfigureMockMvc
@Transactional
class HolidayAdminControllerTest extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private HolidayRepository repo;
    @Autowired private HolidayService holidayService;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void seed() {
        repo.deleteAll();
        Holiday h = new Holiday();
        h.setCode("IN_REPUBLIC_DAY");
        h.setName("Republic Day");
        h.setDate(LocalDate.of(2026, 1, 26));
        h.setScope(HolidayScope.NATIONAL);
        h.setRecurrenceType(HolidayRecurrenceType.FIXED_DATE);
        h.setActive(true);
        repo.save(h);
        holidayService.evictAll();
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/admin/holidays").param("scope", "NATIONAL").param("year", "2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "OTHER_PERMISSION")
    void wrongAuthority_returns403() throws Exception {
        mvc.perform(get("/api/admin/holidays").param("scope", "NATIONAL").param("year", "2026"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "HOLIDAY_VIEW")
    void list_withViewAuthority_returns200_andPayload() throws Exception {
        mvc.perform(get("/api/admin/holidays").param("scope", "NATIONAL").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("IN_REPUBLIC_DAY"));
    }

    @Test
    @WithMockUser(authorities = "HOLIDAY_VIEW")
    void post_withOnlyViewAuthority_returns403() throws Exception {
        HolidayDto dto = new HolidayDto(
                null, "IN_X", "Test", LocalDate.of(2026, 12, 31),
                HolidayScope.NATIONAL, null, HolidayRecurrenceType.FIXED_DATE, true);
        mvc.perform(post("/api/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "HOLIDAY_MANAGE")
    void post_withManageAuthority_returns201() throws Exception {
        HolidayDto dto = new HolidayDto(
                null, "IN_X", "Test", LocalDate.of(2026, 12, 31),
                HolidayScope.NATIONAL, null, HolidayRecurrenceType.FIXED_DATE, true);
        mvc.perform(post("/api/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("IN_X"));
    }

    @Test
    @WithMockUser(authorities = "HOLIDAY_MANAGE")
    void post_withMissingName_returns400() throws Exception {
        String bad = """
            {"code":"IN_X","date":"2026-12-31","scope":"NATIONAL","recurrenceType":"FIXED_DATE","active":true}
            """;
        mvc.perform(post("/api/admin/holidays").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest());
    }
}
