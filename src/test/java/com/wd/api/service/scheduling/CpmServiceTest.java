package com.wd.api.service.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.model.scheduling.TaskPredecessor;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.TaskPredecessorRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class CpmServiceTest extends TestcontainersPostgresBase {

    @Autowired private CpmService cpm;
    @Autowired private TaskRepository tasks;
    @Autowired private TaskPredecessorRepository preds;
    @Autowired private CustomerProjectRepository projects;
    @Autowired private ProjectScheduleConfigRepository configRepo;
    @Autowired private HolidayService holidayService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private jakarta.persistence.EntityManager em;

    /** Build a CpmService whose "today" is pinned to {@code fixedToday} (system zone). */
    private CpmService cpmAt(LocalDate fixedToday) {
        Clock fixed = Clock.fixed(
                fixedToday.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        CpmService s = new CpmService(tasks, preds, projects, configRepo, holidayService, jdbc, fixed);
        // CpmService uses @PersistenceContext on em — Spring would inject it on the
        // bean, but our manual instance needs it set reflectively for in-progress
        // task computations that flush via the EntityManager.
        try {
            var f = CpmService.class.getDeclaredField("em");
            f.setAccessible(true);
            f.set(s, em);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return s;
    }

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
    void cycle_defensivelyThrows_whenGraphContainsOne() {
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
    void completedTask_actualEndOnly_esDerivedFromActualEndMinusDuration() {
        CustomerProject p = newProject(LocalDate.of(2026, 6, 1));
        Task a = newTask(p, "A", 5);
        a.setActualEndDate(LocalDate.of(2026, 6, 10));   // completed
        a.setActualStartDate(null);                       // not recorded
        tasks.save(a);

        cpm.recompute(p.getId());
        Task aOut = tasks.findById(a.getId()).orElseThrow();

        assertThat(aOut.getEfDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(aOut.getEsDate())
                .isEqualTo(WorkingDayCalculator.subtractWorkingDays(
                        LocalDate.of(2026, 6, 10), 5, java.util.Set.of(), false));
    }

    @Test
    void completedTask_bothActuals_esEqualsActualStart_efEqualsActualEnd() {
        CustomerProject p = newProject(LocalDate.of(2026, 6, 1));
        Task a = newTask(p, "A", 5);
        a.setActualStartDate(LocalDate.of(2026, 6, 2));
        a.setActualEndDate(LocalDate.of(2026, 6, 9));
        tasks.save(a);

        cpm.recompute(p.getId());
        Task aOut = tasks.findById(a.getId()).orElseThrow();

        assertThat(aOut.getEsDate()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(aOut.getEfDate()).isEqualTo(LocalDate.of(2026, 6, 9));
    }

    @Test
    void inProgressTask_actualStartOnly_efIsTodayPlusRemaining() {
        // Task started 3 working days ago with 5-day duration -> 2 remaining.
        // Pin "today" to a fixed Monday so the math is independent of the day the
        // suite runs on — earlier draft used LocalDate.now() and failed on Sundays
        // because subtractWorkingDays/workingDaysBetween's anchor convention diverged
        // when "today" was itself a non-working day.
        LocalDate today = LocalDate.of(2026, 6, 8); // Monday, no holidays
        LocalDate started = WorkingDayCalculator.subtractWorkingDays(today, 3, java.util.Set.of(), false);

        CustomerProject p = newProject(started);
        Task a = newTask(p, "A", 5);
        a.setActualStartDate(started);
        a.setActualEndDate(null); // still in progress
        tasks.save(a);

        cpmAt(today).recompute(p.getId());
        Task aOut = tasks.findById(a.getId()).orElseThrow();

        assertThat(aOut.getEsDate()).isEqualTo(started);
        // EF = today + 2 remaining working days
        assertThat(aOut.getEfDate())
                .isEqualTo(WorkingDayCalculator.addWorkingDays(today, 2, java.util.Set.of(), false));
    }

    @Test
    void slippedPastDeadline_producesNegativeFloat_andIsCritical() {
        // Construct a scenario where a non-leaf task's derived ls falls before
        // its es — i.e., negative total float.
        //
        // Layout:
        //   A (parallel, no edges, duration 5d, planned only) → drives projectFinish.
        //   B → C chain (B precedes C). Both B and C have actualEndDate set, but
        //        C's actualEnd is EARLIER than B's actualEnd (sloppy real-world data,
        //        or a long-duration task whose successor was crashed early).
        //
        // Engineer the dates so that during the backward pass:
        //   projectFinish = A.ef (the long parallel path).
        //   C is a leaf → C.lf = projectFinish, C.ls = projectFinish - duration_C.
        //   B's lf = C.ls - lag = projectFinish - duration_C.
        //   With duration_C large enough, B.lf < B.ef, hence B.ls < B.es. Negative float.
        //
        // Concrete numbers (project starts Mon 2026-06-01, no holidays, Sunday off):
        //   A: planned duration 30d → A.ef ~ 2026-07-08.
        //   B: planned duration 2d, actualEnd 2026-06-09 (B.ef=2026-06-09, B.es=2026-06-06ish).
        //   C: planned duration 25d, actualEnd 2026-06-05 (earlier than B's). C.ef=2026-06-05,
        //      C.es = 2026-06-05 - 25wd. C.lf = projectFinish = ~2026-07-08;
        //      C.ls = projectFinish - 25wd ≈ 2026-06-04.
        //   B.lf = C.ls = ~2026-06-04 — which is BEFORE B.ef=2026-06-09.
        //   B.ls = B.lf - 2wd ≈ 2026-06-02, while B.es = ~2026-06-06.
        //   ⇒ B.ls < B.es → negative float. Old code throws IllegalArgumentException
        //     in WorkingDayCalculator.workingDaysBetween(B.es, B.ls, ...).
        CustomerProject p = newProject(LocalDate.of(2026, 6, 1));
        newTask(p, "A", 30);  // long parallel path → drives projectFinish
        Task b = newTask(p, "B", 2);
        Task c = newTask(p, "C", 25);
        b.setActualEndDate(LocalDate.of(2026, 6, 9));
        c.setActualEndDate(LocalDate.of(2026, 6, 5)); // earlier than B's actualEnd
        tasks.save(b);
        tasks.save(c);
        link(c, b, 0);

        cpm.recompute(p.getId());

        Task bOut = tasks.findById(b.getId()).orElseThrow();
        // Negative float on B (slipped past its derived deadline).
        assertThat(bOut.getTotalFloatDays()).as("B float should be negative").isLessThan(0);
        assertThat(bOut.getIsCritical()).as("negative-float task is critical").isTrue();
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
