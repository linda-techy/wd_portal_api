package com.wd.api.service.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@Transactional
class TaskPredecessorServiceCpmHookTest extends TestcontainersPostgresBase {

    @Autowired private TaskPredecessorService predecessorService;
    @Autowired private TaskRepository tasks;
    @Autowired private CustomerProjectRepository projects;

    @SpyBean private CpmService cpm;

    @Test
    void replacePredecessors_triggersCpmRecompute() {
        CustomerProject p = new CustomerProject();
        p.setName("hook " + UUID.randomUUID());
        p.setLocation("Test");
        p.setProjectUuid(UUID.randomUUID());
        p.setStartDate(LocalDate.of(2026, 6, 1));
        p = projects.save(p);

        Task a = newTask(p, "A");
        Task b = newTask(p, "B");

        predecessorService.replacePredecessors(
                b.getId(),
                List.of(new TaskPredecessorService.PredecessorEntry(a.getId(), 0)));

        verify(cpm, atLeastOnce()).recompute(p.getId());
    }

    private Task newTask(CustomerProject p, String title) {
        Task t = new Task();
        t.setTitle(title);
        t.setStatus(Task.TaskStatus.PENDING);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setProject(p);
        t.setDueDate(LocalDate.of(2030, 12, 31));
        t.setStartDate(LocalDate.of(2026, 6, 1));
        t.setEndDate(LocalDate.of(2026, 6, 5));
        return tasks.save(t);
    }
}
