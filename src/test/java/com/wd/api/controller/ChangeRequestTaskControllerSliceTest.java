package com.wd.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.dto.changerequest.ChangeRequestTaskCreateRequest;
import com.wd.api.dto.changerequest.ChangeRequestTaskPredecessorRequest;
import com.wd.api.dto.changerequest.ChangeRequestTaskUpdateRequest;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.changerequest.ChangeRequestTask;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.ProjectVariationRepository;
import com.wd.api.repository.changerequest.ChangeRequestTaskRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for ChangeRequestTaskController over a real Postgres
 * testcontainer. Mirrors ProjectVariationControllerSliceTest convention:
 * exercises the full security + validation + service stack.
 */
@AutoConfigureMockMvc
class ChangeRequestTaskControllerSliceTest extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerProjectRepository projectRepo;
    @Autowired private ProjectVariationRepository crRepo;
    @Autowired private ChangeRequestTaskRepository taskRepo;

    private Long projectId;
    private Long crId;

    @BeforeEach
    void setUp() {
        CustomerProject p = new CustomerProject();
        p.setName("crt-" + UUID.randomUUID());
        p.setLocation("L");
        p.setProjectUuid(UUID.randomUUID());
        projectId = projectRepo.save(p).getId();

        ProjectVariation cr = ProjectVariation.builder()
                .description("d").estimatedAmount(new BigDecimal("1"))
                .status(VariationStatus.DRAFT).build();
        cr.setProject(projectRepo.findById(projectId).orElseThrow());
        crId = crRepo.save(cr).getId();
    }

    @Test
    @WithMockUser(authorities = "CR_SUBMIT")
    void postTask_succeeds_withCrSubmitAuthority() throws Exception {
        ChangeRequestTaskCreateRequest req = new ChangeRequestTaskCreateRequest();
        req.setName("Add 2 rooms slab");
        req.setDurationDays(3);

        mvc.perform(post("/api/projects/{p}/change-requests/{c}/tasks", projectId, crId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Add 2 rooms slab"))
                .andExpect(jsonPath("$.sequence").value(1));
    }

    @Test
    @WithMockUser(authorities = "PROJECT_VIEW")
    void postTask_forbids_withoutCrSubmit() throws Exception {
        ChangeRequestTaskCreateRequest req = new ChangeRequestTaskCreateRequest();
        req.setName("X"); req.setDurationDays(1);
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/tasks", projectId, crId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void postTask_unauthorized_withoutAnyUser() throws Exception {
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/tasks", projectId, crId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CR_SUBMIT")
    void postTask_validation_rejectsBlankName() throws Exception {
        ChangeRequestTaskCreateRequest req = new ChangeRequestTaskCreateRequest();
        req.setDurationDays(3);  // name omitted
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/tasks", projectId, crId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CR_SUBMIT")
    void patchTask_succeeds_withCrSubmit() throws Exception {
        Long taskId = seedTask("Old", 1);

        ChangeRequestTaskUpdateRequest req = new ChangeRequestTaskUpdateRequest();
        req.setName("New");

        mvc.perform(patch("/api/projects/{p}/change-requests/{c}/tasks/{t}", projectId, crId, taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"));
    }

    @Test
    @WithMockUser(authorities = "CR_SUBMIT")
    void deleteTask_succeeds_withCrSubmit() throws Exception {
        Long taskId = seedTask("ToDelete", 1);
        mvc.perform(delete("/api/projects/{p}/change-requests/{c}/tasks/{t}", projectId, crId, taskId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "CR_SUBMIT")
    void postPredecessor_succeeds() throws Exception {
        Long predId = seedTask("Pred", 1);
        Long succId = seedTask("Succ", 2);

        ChangeRequestTaskPredecessorRequest req = new ChangeRequestTaskPredecessorRequest();
        req.setSuccessorCrTaskId(succId);
        req.setPredecessorCrTaskId(predId);

        mvc.perform(post("/api/projects/{p}/change-requests/{c}/tasks/predecessors", projectId, crId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successorCrTaskId").value(succId))
                .andExpect(jsonPath("$.predecessorCrTaskId").value(predId));
    }

    @Test
    @WithMockUser(authorities = "TASK_VIEW")
    void getTasks_succeeds_withTaskView() throws Exception {
        seedTask("X", 1);
        mvc.perform(get("/api/projects/{p}/change-requests/{c}/tasks", projectId, crId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("X"));
    }

    private Long seedTask(String name, int seq) {
        ChangeRequestTask t = new ChangeRequestTask();
        t.setName(name);
        t.setSequence(seq);
        t.setDurationDays(1);
        t.setChangeRequest(crRepo.findById(crId).orElseThrow());
        return taskRepo.save(t).getId();
    }
}
