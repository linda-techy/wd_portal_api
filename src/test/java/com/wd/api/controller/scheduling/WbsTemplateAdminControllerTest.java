package com.wd.api.controller.scheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wd.api.model.enums.FloorLoop;
import com.wd.api.service.scheduling.WbsTemplateService;
import com.wd.api.service.scheduling.dto.WbsTemplateDto;
import com.wd.api.service.scheduling.dto.WbsTemplatePhaseDto;
import com.wd.api.service.scheduling.dto.WbsTemplateTaskDto;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class WbsTemplateAdminControllerTest extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private WbsTemplateService service;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private WbsTemplateDto fixtureDto(String code, String name) {
        return new WbsTemplateDto(
                null, code, "RESIDENTIAL", name, "desc",
                null, null,
                List.of(new WbsTemplatePhaseDto(null, 1, "Phase", null, false,
                        List.of(new WbsTemplateTaskDto(-1L, 1, "Task", null,
                                3, null, false, false, FloorLoop.NONE, null, List.of())))),
                null);
    }

    @Test
    void unauthenticated_list_returns401() throws Exception {
        mvc.perform(get("/api/wbs/templates"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "OTHER_PERM")
    void noAuthority_list_returns403() throws Exception {
        mvc.perform(get("/api/wbs/templates"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "WBS_TEMPLATE_VIEW")
    void list_withViewAuthority_returns200() throws Exception {
        service.create(fixtureDto("CTRL_LIST", "List Test"));
        mvc.perform(get("/api/wbs/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(authorities = "WBS_TEMPLATE_VIEW")
    void getById_returns200() throws Exception {
        WbsTemplateDto created = service.create(fixtureDto("CTRL_GET", "Get Test"));
        mvc.perform(get("/api/wbs/templates/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CTRL_GET"));
    }

    @Test
    @WithMockUser(authorities = "WBS_TEMPLATE_VIEW")
    void getByCode_returns200() throws Exception {
        service.create(fixtureDto("CTRL_BYCODE", "ByCode Test"));
        mvc.perform(get("/api/wbs/templates/by-code/{code}", "CTRL_BYCODE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CTRL_BYCODE"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @WithMockUser(authorities = "WBS_TEMPLATE_VIEW")
    void post_withOnlyView_returns403() throws Exception {
        mvc.perform(post("/api/wbs/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(fixtureDto("CTRL_POST", "Post Test"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "WBS_TEMPLATE_MANAGE")
    void post_withManage_returns201() throws Exception {
        mvc.perform(post("/api/wbs/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(fixtureDto("CTRL_CREATE", "Created"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CTRL_CREATE"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @WithMockUser(authorities = "WBS_TEMPLATE_MANAGE")
    void put_withManage_returns200_andBumpsVersion() throws Exception {
        WbsTemplateDto created = service.create(fixtureDto("CTRL_PUT", "v1"));
        mvc.perform(put("/api/wbs/templates/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(fixtureDto("CTRL_PUT", "v2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.name").value("v2"));
    }

    @Test
    @WithMockUser(authorities = "WBS_TEMPLATE_MANAGE")
    void delete_withManage_returns204() throws Exception {
        WbsTemplateDto created = service.create(fixtureDto("CTRL_DEL", "deleted"));
        mvc.perform(delete("/api/wbs/templates/{id}", created.id()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "WBS_TEMPLATE_VIEW")
    void getById_unknown_returns404() throws Exception {
        mvc.perform(get("/api/wbs/templates/{id}", 999_999L))
                .andExpect(status().isNotFound());
    }
}
