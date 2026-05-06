package com.wd.api.service.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.model.scheduling.TaskPredecessor;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.TaskPredecessorRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class CpmServicePerformanceTest extends TestcontainersPostgresBase {

    @Autowired private CpmService cpm;
    @Autowired private TaskRepository tasks;
    @Autowired private TaskPredecessorRepository preds;
    @Autowired private CustomerProjectRepository projects;

    @Test
    void recompute_100TaskChain_underOneHundredMillis() {
        CustomerProject p = new CustomerProject();
        p.setName("perf " + UUID.randomUUID());
        p.setLocation("Test");
        p.setProjectUuid(UUID.randomUUID());
        p.setStartDate(LocalDate.of(2026, 6, 1));
        p = projects.save(p);

        Long prev = null;
        for (int i = 0; i < 100; i++) {
            Task t = new Task();
            t.setTitle("task-" + i);
            t.setStatus(Task.TaskStatus.PENDING);
            t.setPriority(Task.TaskPriority.MEDIUM);
            t.setProject(p);
            t.setDueDate(LocalDate.of(2030, 12, 31));
            t.setStartDate(LocalDate.of(2026, 6, 1));
            t.setEndDate(LocalDate.of(2026, 6, 2));
            tasks.save(t);
            if (prev != null) preds.save(new TaskPredecessor(t.getId(), prev, 0));
            prev = t.getId();
        }

        // Warm-up to amortise JIT / Hibernate first-call cost.
        cpm.recompute(p.getId());

        long start = System.currentTimeMillis();
        cpm.recompute(p.getId());
        long duration = System.currentTimeMillis() - start;

        assertThat(duration)
                .as("100-task chain CPM recompute should be under 100ms; took %dms", duration)
                .isLessThan(100);
    }
}
