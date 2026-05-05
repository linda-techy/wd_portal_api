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
class CpmServiceTest extends TestcontainersPostgresBase {

    @Autowired private CpmService cpm;
    @Autowired private TaskRepository tasks;
    @Autowired private TaskPredecessorRepository preds;
    @Autowired private CustomerProjectRepository projects;

    private CustomerProject newProject(LocalDate start) {
        CustomerProject p = new CustomerProject();
        p.setName("cpm-test " + UUID.randomUUID());
        p.setLocation("Test");
        p.setProjectUuid(UUID.randomUUID());
        p.setStartDate(start);
        return projects.save(p);
    }

    private Task newTask(CustomerProject p, String title, int durationDays) {
        Task t = new Task();
        t.setTitle(title);
        t.setStatus(Task.TaskStatus.PENDING);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setProject(p);
        t.setDueDate(LocalDate.of(2030, 12, 31));
        // Convention for CPM input: planned duration is endDate - startDate
        // working days; the seed start/end below carry that meaning.
        LocalDate start = p.getStartDate() == null ? LocalDate.now() : p.getStartDate();
        t.setStartDate(start);
        t.setEndDate(WorkingDayCalculator.addWorkingDays(start, durationDays, java.util.Set.of(), false));
        return tasks.save(t);
    }

    private void link(Task successor, Task predecessor, int lagDays) {
        preds.save(new TaskPredecessor(successor.getId(), predecessor.getId(), lagDays));
    }

    @Test
    void linearChain_AtoBtoC_computesExactEsLsEfLf() {
        // Project starts Mon 2026-06-01. Durations 5/3/2 working days, no holidays, no actuals.
        // Sundays excluded; sundayWorking=false (default).
        CustomerProject p = newProject(LocalDate.of(2026, 6, 1));
        Task a = newTask(p, "A", 5);
        Task b = newTask(p, "B", 3);
        Task c = newTask(p, "C", 2);
        link(b, a, 0);
        link(c, b, 0);

        cpm.recompute(p.getId());

        Task aOut = tasks.findById(a.getId()).orElseThrow();
        Task bOut = tasks.findById(b.getId()).orElseThrow();
        Task cOut = tasks.findById(c.getId()).orElseThrow();

        // Forward pass
        assertThat(aOut.getEsDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(aOut.getEfDate()).isEqualTo(LocalDate.of(2026, 6, 6)); // +5 wd skipping Sun 7th not in window
        assertThat(bOut.getEsDate()).isEqualTo(aOut.getEfDate());
        assertThat(cOut.getEsDate()).isEqualTo(bOut.getEfDate());

        // Backward pass: linear chain -> all on critical path -> float = 0
        assertThat(aOut.getTotalFloatDays()).isZero();
        assertThat(bOut.getTotalFloatDays()).isZero();
        assertThat(cOut.getTotalFloatDays()).isZero();
        assertThat(aOut.getIsCritical()).isTrue();
        assertThat(bOut.getIsCritical()).isTrue();
        assertThat(cOut.getIsCritical()).isTrue();

        // ls == es and lf == ef on the critical path
        assertThat(aOut.getLsDate()).isEqualTo(aOut.getEsDate());
        assertThat(cOut.getLfDate()).isEqualTo(cOut.getEfDate());
    }
}
