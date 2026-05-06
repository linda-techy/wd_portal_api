package com.wd.api.service.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.GanttService;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@Transactional
class GanttServiceCpmHookTest extends TestcontainersPostgresBase {

    @Autowired private GanttService ganttService;
    @Autowired private TaskRepository tasks;
    @Autowired private CustomerProjectRepository projects;

    @SpyBean private CpmService cpm;

    @Test
    void updateTaskSchedule_triggersCpmRecompute() {
        CustomerProject p = new CustomerProject();
        p.setName("gantt-hook " + UUID.randomUUID());
        p.setLocation("Test");
        p.setProjectUuid(UUID.randomUUID());
        p.setStartDate(LocalDate.of(2026, 6, 1));
        p = projects.save(p);

        Task t = new Task();
        t.setTitle("X");
        t.setStatus(Task.TaskStatus.PENDING);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setProject(p);
        t.setDueDate(LocalDate.of(2030, 12, 31));
        t = tasks.save(t);

        ganttService.updateTaskSchedule(
                t.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), 0, null);

        verify(cpm, atLeastOnce()).recompute(p.getId());
    }
}
