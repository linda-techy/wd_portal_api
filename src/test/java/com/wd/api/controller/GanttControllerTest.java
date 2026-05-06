package com.wd.api.controller;

import com.wd.api.model.Task;
import com.wd.api.service.GanttService;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for GanttController.PUT /tasks/{id}/schedule.
 *
 * <p>Specifically verifies that the legacy {@code dependsOnTaskId}
 * request-body field is silently ignored after S2 PR2 dropped the
 * column. Predecessor edits flow through the dedicated PUT
 * /tasks/{id}/predecessors endpoint added in S1.
 */
@AutoConfigureMockMvc
class GanttControllerTest extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @MockitoBean private GanttService ganttService;

    @Test
    @WithMockUser(authorities = {"TASK_EDIT"})
    void put_schedule_silentlyIgnoresLegacyDependsOnTaskIdField() throws Exception {
        Task updated = new Task();
        updated.setId(1L);
        updated.setStartDate(LocalDate.of(2026, 6, 1));
        updated.setEndDate(LocalDate.of(2026, 6, 10));
        when(ganttService.updateTaskSchedule(eq(1L), any(), any(), any())).thenReturn(updated);

        String body = "{ \"startDate\": \"2026-06-01\", \"endDate\": \"2026-06-10\", " +
                      "\"progressPercent\": 0, \"dependsOnTaskId\": 99 }";
        mvc.perform(put("/api/projects/42/tasks/1/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Verify the service was called with the new 4-arg signature — the
        // legacy dependsOnTaskId was not threaded through.
        verify(ganttService).updateTaskSchedule(eq(1L), any(), any(), any());
    }
}
