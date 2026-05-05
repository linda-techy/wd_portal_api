package com.wd.api.controller.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.TaskRepository;
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
class MonsoonWarningControllerTest extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private CustomerProjectRepository projects;
    @Autowired private TaskRepository tasks;

    private CustomerProject newProject() {
        CustomerProject p = new CustomerProject();
        p.setName("monsoon-ctrl " + UUID.randomUUID());
        p.setLocation("Test");
        p.setProjectUuid(UUID.randomUUID());
        return projects.save(p);
    }

    private void addMonsoonTask(CustomerProject p, String title) {
        Task t = new Task();
        t.setTitle(title);
        t.setStatus(Task.TaskStatus.PENDING);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setProject(p);
        t.setStartDate(LocalDate.of(2026, 7, 1));
        t.setEndDate(LocalDate.of(2026, 7, 30));
        t.setDueDate(LocalDate.of(2026, 7, 30));
        t.setMonsoonSensitive(true);
        tasks.save(t);
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        CustomerProject p = newProject();
        mvc.perform(get("/api/projects/{id}/schedule/warnings", p.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "OTHER_PERM")
    void noAuthority_returns403() throws Exception {
        CustomerProject p = newProject();
        mvc.perform(get("/api/projects/{id}/schedule/warnings", p.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "MONSOON_WARNING_VIEW")
    void list_withAuthority_returnsArrayWithWarnings() throws Exception {
        CustomerProject p = newProject();
        addMonsoonTask(p, "Slab Casting");

        mvc.perform(get("/api/projects/{id}/schedule/warnings", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].taskName").value("Slab Casting"));
    }

    @Test
    @WithMockUser(authorities = "MONSOON_WARNING_VIEW")
    void list_emptyWhenNoMonsoonTasks() throws Exception {
        CustomerProject p = newProject();
        // no tasks added
        mvc.perform(get("/api/projects/{id}/schedule/warnings", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
