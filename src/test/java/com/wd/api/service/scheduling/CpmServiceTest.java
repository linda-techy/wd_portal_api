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

    @Test
    void diamond_AtoBC_thenBCtoD_longerBranchIsCriticalAndShortHasFloat() {
        // A->B (5d), A->C (8d), B->D, C->D (D=2d). C-branch longer -> C critical, B has float = 8-5 = 3.
        CustomerProject p = newProject(LocalDate.of(2026, 6, 1));
        Task a = newTask(p, "A", 4);
        Task b = newTask(p, "B", 5);
        Task c = newTask(p, "C", 8);
        Task d = newTask(p, "D", 2);
        link(b, a, 0);
        link(c, a, 0);
        link(d, b, 0);
        link(d, c, 0);

        cpm.recompute(p.getId());

        Task bOut = tasks.findById(b.getId()).orElseThrow();
        Task cOut = tasks.findById(c.getId()).orElseThrow();
        assertThat(cOut.getIsCritical()).as("longer branch C is critical").isTrue();
        assertThat(bOut.getIsCritical()).as("shorter branch B is not critical").isFalse();
        assertThat(bOut.getTotalFloatDays()).isEqualTo(3);
        assertThat(cOut.getTotalFloatDays()).isZero();
    }

    @Test
    void laggedPredecessor_BesEqualsAefPlusLag() {
        // A->B with 2-day lag.
        CustomerProject p = newProject(LocalDate.of(2026, 6, 1));
        Task a = newTask(p, "A", 3);
        Task b = newTask(p, "B", 2);
        link(b, a, 2);

        cpm.recompute(p.getId());

        Task aOut = tasks.findById(a.getId()).orElseThrow();
        Task bOut = tasks.findById(b.getId()).orElseThrow();
        // B.es = A.ef + 2 working days
        assertThat(bOut.getEsDate())
                .isEqualTo(WorkingDayCalculator.addWorkingDays(aOut.getEfDate(), 2, java.util.Set.of(), false));
    }

    @Test
    void chainAcrossSunday_efSkipsSunday() {
        // A->B starting Friday with 1d duration each. A finishes Friday (or skips into Mon).
        // Goal: B's EF must skip the intervening Sunday.
        CustomerProject p = newProject(LocalDate.of(2026, 6, 5)); // Friday
        Task a = newTask(p, "A", 1);
        Task b = newTask(p, "B", 1);
        link(b, a, 0);

        cpm.recompute(p.getId());

        Task bOut = tasks.findById(b.getId()).orElseThrow();
        // Sunday 2026-06-07 must NOT appear in any es/ef.
        assertThat(bOut.getEsDate()).isNotEqualTo(LocalDate.of(2026, 6, 7));
        assertThat(bOut.getEfDate()).isNotEqualTo(LocalDate.of(2026, 6, 7));
    }

    @Test
    void cycle_definsivelyThrows_whenGraphContainsOne() {
        // Bypass S1's validator by inserting raw rows directly.
        CustomerProject p = newProject(LocalDate.of(2026, 6, 1));
        Task a = newTask(p, "A", 2);
        Task b = newTask(p, "B", 2);
        preds.save(new TaskPredecessor(b.getId(), a.getId(), 0));
        preds.save(new TaskPredecessor(a.getId(), b.getId(), 0)); // cycle

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> cpm.recompute(p.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void multiLeaf_bothLeavesEndAtProjectFinish() {
        // A->B and A->C with B and C as parallel leaves of equal length.
        CustomerProject p = newProject(LocalDate.of(2026, 6, 1));
        Task a = newTask(p, "A", 3);
        Task b = newTask(p, "B", 4);
        Task c = newTask(p, "C", 4);
        link(b, a, 0);
        link(c, a, 0);

        cpm.recompute(p.getId());

        Task bOut = tasks.findById(b.getId()).orElseThrow();
        Task cOut = tasks.findById(c.getId()).orElseThrow();
        assertThat(bOut.getLfDate()).isEqualTo(cOut.getLfDate());
        assertThat(bOut.getLfDate()).isEqualTo(bOut.getEfDate()); // both leaves at project finish
    }
}
