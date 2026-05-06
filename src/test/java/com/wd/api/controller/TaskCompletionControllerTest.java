package com.wd.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.dto.scheduling.PendingApprovalRowDto;
import com.wd.api.dto.scheduling.RejectCompletionRequest;
import com.wd.api.model.Task;
import com.wd.api.repository.SiteReportRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.scheduling.TaskCompletionService;
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

/**
 * MockMvc tests for {@link TaskCompletionController}. Mocks the
 * TaskCompletionService bean so we exercise URL routing, @PreAuthorize gates,
 * and bean-validation only — not the FSM (covered by
 * {@code TaskCompletionServiceTest}).
 */
@AutoConfigureMockMvc
class TaskCompletionControllerTest extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockitoBean private TaskCompletionService service;
    @MockitoBean private TaskRepository taskRepo;
    @MockitoBean private SiteReportRepository siteReportRepo;

    private Task taskWithStatus(Task.TaskStatus s) {
        Task t = new Task();
        t.setId(42L);
        t.setTitle("Test task");
        t.setStatus(s);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setDueDate(LocalDate.now().plusDays(1));
        return t;
    }

    // ----- /mark-complete -----

    @Test
    @WithMockUser(authorities = {"TASK_EDIT"})
    void markComplete_happyPath_returns200() throws Exception {
        when(service.markComplete(eq(42L), any()))
                .thenReturn(taskWithStatus(Task.TaskStatus.COMPLETED));
        mvc.perform(post("/api/tasks/42/mark-complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(authorities = {})
    void markComplete_withoutTaskEdit_returns403() throws Exception {
        mvc.perform(post("/api/tasks/42/mark-complete"))
                .andExpect(status().isForbidden());
    }

    @Test
    void markComplete_unauthenticated_returns401() throws Exception {
        mvc.perform(post("/api/tasks/42/mark-complete"))
                .andExpect(status().isUnauthorized());
    }

    // ----- /approve-completion -----

    @Test
    @WithMockUser(authorities = {"TASK_COMPLETION_APPROVE"})
    void approveCompletion_happyPath_returns200() throws Exception {
        when(service.approveCompletion(eq(42L), any()))
                .thenReturn(taskWithStatus(Task.TaskStatus.COMPLETED));
        mvc.perform(post("/api/tasks/42/approve-completion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(authorities = {"TASK_EDIT"})
    void approveCompletion_withoutPermission_returns403() throws Exception {
        mvc.perform(post("/api/tasks/42/approve-completion"))
                .andExpect(status().isForbidden());
    }

    // ----- /reject-completion -----

    @Test
    @WithMockUser(authorities = {"TASK_COMPLETION_APPROVE"})
    void rejectCompletion_withReason_returns200() throws Exception {
        when(service.rejectCompletion(eq(42L), any(), eq("Photo blurry")))
                .thenReturn(taskWithStatus(Task.TaskStatus.IN_PROGRESS));
        mvc.perform(post("/api/tasks/42/reject-completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RejectCompletionRequest("Photo blurry"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(authorities = {"TASK_COMPLETION_APPROVE"})
    void rejectCompletion_blankReason_returns400() throws Exception {
        mvc.perform(post("/api/tasks/42/reject-completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RejectCompletionRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"TASK_COMPLETION_APPROVE"})
    void rejectCompletion_tooShortReason_returns400() throws Exception {
        mvc.perform(post("/api/tasks/42/reject-completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RejectCompletionRequest("hi"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"TASK_EDIT"})
    void rejectCompletion_withoutPermission_returns403() throws Exception {
        mvc.perform(post("/api/tasks/42/reject-completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RejectCompletionRequest("Photo blurry"))))
                .andExpect(status().isForbidden());
    }

    // ----- /pending-pm-approval -----

    @Test
    @WithMockUser(authorities = {"TASK_COMPLETION_APPROVE"})
    void pendingApprovalInbox_returnsRows() throws Exception {
        when(service.findPendingApprovalsForUser(any())).thenReturn(List.of(
                new PendingApprovalRowDto(42L, "Beam casting", 7L, "Villa Kochi",
                        LocalDate.of(2026, 5, 4), "/api/storage/site-reports/42/abc.jpg")));
        mvc.perform(get("/api/tasks/pending-pm-approval"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(42))
                .andExpect(jsonPath("$[0].taskTitle").value("Beam casting"))
                .andExpect(jsonPath("$[0].completionPhotoUrl")
                        .value("/api/storage/site-reports/42/abc.jpg"));
    }

    @Test
    @WithMockUser(authorities = {"TASK_EDIT"})
    void pendingApprovalInbox_withoutPermission_returns403() throws Exception {
        mvc.perform(get("/api/tasks/pending-pm-approval"))
                .andExpect(status().isForbidden());
    }
}
