package com.wd.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.dto.scheduling.PredecessorListRequest;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class TaskPredecessorControllerTest extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void unauthenticated_returns401() throws Exception {
        PredecessorListRequest req = new PredecessorListRequest(List.of(
                new PredecessorListRequest.Entry(1L, 0)));
        mvc.perform(put("/api/tasks/2/predecessors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "OTHER")
    void wrongAuthority_returns403() throws Exception {
        PredecessorListRequest req = new PredecessorListRequest(List.of(
                new PredecessorListRequest.Entry(1L, 0)));
        mvc.perform(put("/api/tasks/2/predecessors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "GANTT_EDIT")
    void put_cycleRejected_returns400_onSelfLoop() throws Exception {
        // Self-loop is rejected before any DB access — no need for fixture data.
        PredecessorListRequest req = new PredecessorListRequest(List.of(
                new PredecessorListRequest.Entry(2L, 0)));
        mvc.perform(put("/api/tasks/2/predecessors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "GANTT_EDIT")
    void put_invalidPayload_returns400() throws Exception {
        // null predecessorId in entry
        String bad = "{\"predecessors\":[{\"predecessorId\":null,\"lagDays\":0}]}";
        mvc.perform(put("/api/tasks/2/predecessors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bad))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "GANTT_EDIT")
    void put_taskNotFound_returns400() throws Exception {
        // Predecessor and successor are different IDs and no cycle, but the tasks
        // don't exist in the DB so the dual-write step throws IllegalArgumentException
        // which the controller maps to 400.
        PredecessorListRequest req = new PredecessorListRequest(List.of(
                new PredecessorListRequest.Entry(999_999_001L, 0)));
        mvc.perform(put("/api/tasks/999999002/predecessors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
