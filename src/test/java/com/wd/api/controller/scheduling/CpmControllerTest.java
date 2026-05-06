package com.wd.api.controller.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.scheduling.CpmService;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class CpmControllerTest extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private TaskRepository tasks;
    @Autowired private CustomerProjectRepository projects;
    @Autowired private CpmService cpm;

    private CustomerProject seedProjectWithOneTask() {
        CustomerProject p = new CustomerProject();
        p.setName("cpm-ctrl " + UUID.randomUUID());
        p.setLocation("Test");
        p.setProjectUuid(UUID.randomUUID());
        p.setStartDate(LocalDate.of(2026, 6, 1));
        p = projects.save(p);
        Task t = new Task();
        t.setTitle("Slab Casting");
        t.setStatus(Task.TaskStatus.PENDING);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setProject(p);
        t.setDueDate(LocalDate.of(2030, 12, 31));
        t.setStartDate(LocalDate.of(2026, 6, 1));
        t.setEndDate(LocalDate.of(2026, 6, 5));
        tasks.save(t);
        cpm.recompute(p.getId());
        return p;
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        CustomerProject p = seedProjectWithOneTask();
        mvc.perform(get("/api/projects/{id}/cpm", p.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "OTHER_PERM")
    void noAuthority_returns403() throws Exception {
        CustomerProject p = seedProjectWithOneTask();
        mvc.perform(get("/api/projects/{id}/cpm", p.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "TASK_VIEW")
    void withTaskView_returns200WithExpectedShape() throws Exception {
        CustomerProject p = seedProjectWithOneTask();

        mvc.perform(get("/api/projects/{id}/cpm", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(p.getId()))
                .andExpect(jsonPath("$.projectStartDate").value("2026-06-01"))
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.tasks[0].taskName").value("Slab Casting"))
                .andExpect(jsonPath("$.tasks[0].esDate").exists())
                .andExpect(jsonPath("$.criticalTaskIds").isArray());
    }
}
