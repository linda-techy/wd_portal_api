package com.wd.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.dto.CrCostRequest;
import com.wd.api.dto.CrRejectRequest;
import com.wd.api.dto.CrScheduleRequest;
import com.wd.api.dto.changerequest.ChangeRequestMergeResult;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.ProjectVariationRepository;
import com.wd.api.service.changerequest.ChangeRequestMergeService;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class ProjectVariationControllerSliceTest extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;
    @Autowired private CustomerProjectRepository projectRepo;
    @Autowired private PortalUserRepository userRepo;
    @Autowired private ProjectVariationRepository crRepo;
    /**
     * S4 PR2 wires ChangeRequestMergeService into ProjectVariationService.schedule.
     * The slice test only exercises HTTP / security envelopes, so the merge is
     * mocked to a no-op result.
     */
    @MockitoBean private ChangeRequestMergeService mergeService;

    private Long projectId;
    private Long crId;

    @BeforeEach
    void setup() {
        when(mergeService.mergeIntoWbs(anyLong(), anyLong(), any()))
                .thenReturn(new ChangeRequestMergeResult(0, 0, 0, true));

        CustomerProject p = new CustomerProject();
        p.setName("ctrl-" + UUID.randomUUID());
        p.setLocation("L"); p.setProjectUuid(UUID.randomUUID());
        projectId = projectRepo.save(p).getId();

        PortalUser u = new PortalUser();
        u.setEmail("ctrl-u-" + UUID.randomUUID() + "@t");
        u.setFirstName("u"); u.setLastName("u"); u.setPassword("x"); u.setEnabled(true);
        userRepo.save(u);

        ProjectVariation cr = ProjectVariation.builder()
                .description("d").estimatedAmount(new BigDecimal("1"))
                .status(VariationStatus.DRAFT).build();
        cr.setProject(projectRepo.findById(projectId).orElseThrow());
        crId = crRepo.save(cr).getId();
    }

    // ---- 401 anonymous ----

    @Test
    void submit_anon_401() throws Exception {
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/submit", projectId, crId))
                .andExpect(status().isUnauthorized());
    }

    // ---- 403 wrong authority ----

    @Test
    @WithMockUser(authorities = {"PROJECT_VIEW"})
    void submit_lacksCrSubmit_403() throws Exception {
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/submit", projectId, crId))
                .andExpect(status().isForbidden());
    }

    // ---- 200 happy ----

    @Test
    @WithMockUser(authorities = {"CR_SUBMIT"})
    void submit_withAuthority_200() throws Exception {
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/submit", projectId, crId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @WithMockUser(authorities = {"CR_COST"})
    void cost_withAuthority_200() throws Exception {
        // walk to SUBMITTED first via direct repo
        ProjectVariation cr = crRepo.findById(crId).orElseThrow();
        cr.setStatus(VariationStatus.SUBMITTED);
        crRepo.save(cr);

        CrCostRequest req = new CrCostRequest();
        req.setCostImpact(new BigDecimal("125000.00"));
        req.setTimeImpactWorkingDays(12);
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/cost", projectId, crId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COSTED"))
                .andExpect(jsonPath("$.costImpact").value(125000.00))
                .andExpect(jsonPath("$.timeImpactWorkingDays").value(12));
    }

    @Test
    @WithMockUser(authorities = {"CR_COST"})
    void cost_missingTimeImpact_400() throws Exception {
        ProjectVariation cr = crRepo.findById(crId).orElseThrow();
        cr.setStatus(VariationStatus.SUBMITTED);
        crRepo.save(cr);
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/cost", projectId, crId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"costImpact\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"CR_SEND_TO_CUSTOMER"})
    void sendToCustomer_withAuthority_200() throws Exception {
        ProjectVariation cr = crRepo.findById(crId).orElseThrow();
        cr.setStatus(VariationStatus.COSTED);
        crRepo.save(cr);
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/send-to-customer", projectId, crId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CUSTOMER_APPROVAL_PENDING"));
    }

    @Test
    @WithMockUser(authorities = {"CR_SCHEDULE"})
    void schedule_withAuthority_200() throws Exception {
        ProjectVariation cr = crRepo.findById(crId).orElseThrow();
        cr.setStatus(VariationStatus.APPROVED);
        crRepo.save(cr);
        CrScheduleRequest req = new CrScheduleRequest();
        req.setAnchorTaskId(42L);
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/schedule", projectId, crId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @WithMockUser(authorities = {"CR_START"})
    void start_withAuthority_200() throws Exception {
        ProjectVariation cr = crRepo.findById(crId).orElseThrow();
        cr.setStatus(VariationStatus.SCHEDULED);
        crRepo.save(cr);
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/start", projectId, crId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(authorities = {"CR_COMPLETE"})
    void complete_withAuthority_200() throws Exception {
        ProjectVariation cr = crRepo.findById(crId).orElseThrow();
        cr.setStatus(VariationStatus.IN_PROGRESS);
        crRepo.save(cr);
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/complete", projectId, crId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETE"));
    }

    @Test
    @WithMockUser(authorities = {"CR_REJECT"})
    void reject_withAuthorityAndReason_200() throws Exception {
        CrRejectRequest req = new CrRejectRequest();
        req.setReason("scope creep");
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/reject", projectId, crId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("scope creep"));
    }

    @Test
    @WithMockUser(authorities = {"CR_REJECT"})
    void reject_blankReason_400() throws Exception {
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/reject", projectId, crId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"PROJECT_VIEW"})
    void history_withProjectView_200() throws Exception {
        mvc.perform(get("/api/projects/{p}/change-requests/{c}/history", projectId, crId)
                        .param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void history_anon_401() throws Exception {
        mvc.perform(get("/api/projects/{p}/change-requests/{c}/history", projectId, crId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"CR_SCHEDULE"})
    void illegalTransition_DraftToSchedule_returns422() throws Exception {
        // CR is in DRAFT (per @BeforeEach); CR_SCHEDULE permission allows the
        // request through authority gating; the state machine then rejects
        // DRAFT -> SCHEDULED with IllegalStateException, which
        // GlobalExceptionHandler maps to 422 Unprocessable Entity.
        mvc.perform(post("/api/projects/{p}/change-requests/{c}/schedule", projectId, crId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"anchorTaskId\":1}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
