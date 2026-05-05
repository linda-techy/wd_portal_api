package com.wd.api.controller.scheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.enums.FloorLoop;
import com.wd.api.model.scheduling.WbsTemplate;
import com.wd.api.model.scheduling.WbsTemplatePhase;
import com.wd.api.model.scheduling.WbsTemplateTask;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.scheduling.WbsTemplatePhaseRepository;
import com.wd.api.repository.scheduling.WbsTemplateRepository;
import com.wd.api.repository.scheduling.WbsTemplateTaskRepository;
import com.wd.api.service.scheduling.dto.WbsCloneRequest;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectWbsCloneControllerTest extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private WbsTemplateRepository templates;
    @Autowired private WbsTemplatePhaseRepository phases;
    @Autowired private WbsTemplateTaskRepository templateTasks;
    @Autowired private CustomerProjectRepository projects;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private CustomerProject newProject(Integer floors) {
        CustomerProject p = new CustomerProject();
        p.setName("clone-ctrl " + UUID.randomUUID());
        p.setLocation("Test");
        p.setProjectUuid(UUID.randomUUID());
        p.setStartDate(LocalDate.of(2026, 6, 1));
        p.setFloors(floors);
        return projects.save(p);
    }

    private Long buildTemplate() {
        WbsTemplate t = new WbsTemplate();
        t.setCode("CTRL_CLONE_T");
        t.setProjectType("RESIDENTIAL");
        t.setName("Clone Test");
        t = templates.save(t);
        WbsTemplatePhase p = new WbsTemplatePhase();
        p.setTemplate(t); p.setSequence(1); p.setName("phase");
        p = phases.save(p);
        WbsTemplateTask task = new WbsTemplateTask();
        task.setPhase(p); task.setSequence(1); task.setName("Foundation");
        task.setDurationDays(5); task.setFloorLoop(FloorLoop.NONE);
        templateTasks.save(task);
        return t.getId();
    }

    @Test
    @WithMockUser(authorities = "PROJECT_WBS_CLONE")
    void clone_withAuthority_returns200_andSummary() throws Exception {
        Long templateId = buildTemplate();
        CustomerProject project = newProject(2);

        WbsCloneRequest req = new WbsCloneRequest(templateId, 2);
        mvc.perform(post("/api/projects/{projectId}/wbs/clone-from-template", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.milestonesCreated").value(1))
                .andExpect(jsonPath("$.tasksCreated").value(1));
    }

    @Test
    @WithMockUser(authorities = "OTHER_PERM")
    void clone_withoutAuthority_returns403() throws Exception {
        Long templateId = buildTemplate();
        CustomerProject project = newProject(2);
        WbsCloneRequest req = new WbsCloneRequest(templateId, 2);

        mvc.perform(post("/api/projects/{projectId}/wbs/clone-from-template", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PROJECT_WBS_CLONE")
    void clone_missingTemplateId_returns400() throws Exception {
        CustomerProject project = newProject(2);
        WbsCloneRequest req = new WbsCloneRequest(null, 2);

        mvc.perform(post("/api/projects/{projectId}/wbs/clone-from-template", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "PROJECT_WBS_CLONE")
    void clone_floorCountZero_returns400() throws Exception {
        Long templateId = buildTemplate();
        CustomerProject project = newProject(2);
        WbsCloneRequest req = new WbsCloneRequest(templateId, 0);

        mvc.perform(post("/api/projects/{projectId}/wbs/clone-from-template", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "PROJECT_WBS_CLONE")
    void clone_unknownTemplate_returns404() throws Exception {
        CustomerProject project = newProject(2);
        WbsCloneRequest req = new WbsCloneRequest(999_999L, 2);

        mvc.perform(post("/api/projects/{projectId}/wbs/clone-from-template", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}
