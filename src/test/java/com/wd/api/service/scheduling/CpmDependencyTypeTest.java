package com.wd.api.service.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.model.enums.DependencyType;
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

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CPM engine tests for all four dependency types: FS, SS, FF, SF.
 *
 * <p>Conventions:
 * <ul>
 *   <li>Project start: Mon 2026-06-01. No holidays. sundayWorking=false.</li>
 *   <li>Duration provided via startDate/endDate on the task (workingDaysBetween).</li>
 *   <li>Lag = 0 for all edges unless stated otherwise.</li>
 *   <li>All tasks planned (no actuals), so the full CPM forward/backward pass runs.</li>
 * </ul>
 *
 * <p>All four types with lag=0, 2-node chain P→S where P has duration=3wd, S has duration=2wd:
 * <pre>
 *   P starts Mon 2026-06-01, ends Wed 2026-06-03 (3wd: Mon+Tue+Wed).
 *
 *   FS: S.ES = P.EF = 2026-06-03  →  S.EF = addWD(2026-06-03, 2) = 2026-06-05
 *   SS: S.ES = P.ES = 2026-06-01  →  S.EF = addWD(2026-06-01, 2) = 2026-06-03
 *   FF: S.ES = subWD(P.EF, S.dur) = subWD(2026-06-03, 2) = 2026-06-01
 *       S.EF = P.EF = 2026-06-03
 *   SF: S.ES = subWD(P.ES, S.dur) = subWD(2026-06-01, 2) = 2026-05-29 (Fri)
 *       S.EF = addWD(2026-05-29, 2) = 2026-06-01
 * </pre>
 *
 * <p>Backward pass for each 2-node chain (no further successors, so S.LF = projectFinish = S.EF):
 * <pre>
 *   FS: P.LF = subWD(S.LS, lag=0) = subWD(S.LF - S.dur, 0) = S.LS
 *   SS: P.LF = addWD(subWD(S.LS, 0), P.dur) = S.LS + P.dur
 *   FF: P.LF = subWD(S.LF, 0) = S.LF
 *   SF: P.LF = addWD(subWD(S.LF, 0), P.dur) = S.LF + P.dur
 * </pre>
 *
 * <p>Regression: all-FS graph must yield same ES/EF as baseline (existing CpmServiceTest covers this;
 *   this class adds a dedicated regression assertion).
 */
@Transactional
class CpmDependencyTypeTest extends TestcontainersPostgresBase {

    // Mon 2026-06-01 — project anchor for all tests.
    private static final LocalDate PROJECT_START = LocalDate.of(2026, 6, 1);

    @Autowired private CpmService cpm;
    @Autowired private TaskRepository tasks;
    @Autowired private TaskPredecessorRepository preds;
    @Autowired private CustomerProjectRepository projects;
    @Autowired private ProjectScheduleConfigRepository configRepo;
    @Autowired private JdbcTemplate jdbc;

    // ── helpers ───────────────────────────────────────────────────────────────

    private CustomerProject newProject() {
        CustomerProject p = new CustomerProject();
        p.setName("cpm-dep-test " + UUID.randomUUID());
        p.setLocation("Test");
        p.setProjectUuid(UUID.randomUUID());
        p.setStartDate(PROJECT_START);
        return projects.save(p);
    }

    /**
     * Creates a planned task with explicit start/end dates (no actuals).
     * The CPM engine derives duration from workingDaysBetween(start, end).
     */
    private Task task(CustomerProject p, String title, LocalDate start, int durationWd) {
        Task t = new Task();
        t.setTitle(title);
        t.setStatus(Task.TaskStatus.PENDING);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setProject(p);
        t.setDueDate(LocalDate.of(2030, 12, 31));
        t.setStartDate(start);
        t.setEndDate(WorkingDayCalculator.addWorkingDays(start, durationWd, Set.of(), false));
        return tasks.save(t);
    }

    private void edge(Task succ, Task pred, DependencyType type) {
        TaskPredecessor e = new TaskPredecessor(succ.getId(), pred.getId(), 0);
        e.setDepType(type);
        preds.save(e);
    }

    private LocalDate add(LocalDate d, int wd) {
        return WorkingDayCalculator.addWorkingDays(d, wd, Set.of(), false);
    }

    private LocalDate sub(LocalDate d, int wd) {
        return WorkingDayCalculator.subtractWorkingDays(d, wd, Set.of(), false);
    }

    // ── FS regression ─────────────────────────────────────────────────────────

    /**
     * Regression: FS dependency (the original behaviour) is unchanged.
     * P(3wd) → S(2wd): S.ES = P.EF, S.EF = P.EF + 2wd.
     */
    @Test
    void fs_regression_succStartsAfterPredFinish() {
        CustomerProject p = newProject();
        // P: Mon Jun 1 → Wed Jun 3 (3 wd)
        Task predTask = task(p, "P", PROJECT_START, 3);
        // S: 2 wd duration, start from project start (CPM will recompute ES)
        Task succTask = task(p, "S", PROJECT_START, 2);
        edge(succTask, predTask, DependencyType.FS);

        cpm.recompute(p.getId());

        Task pOut = tasks.findById(predTask.getId()).orElseThrow();
        Task sOut = tasks.findById(succTask.getId()).orElseThrow();

        // P: ES=Jun1, EF=Jun3 (3wd from Jun1: Jun2,Jun3 — wait, addWD counts forward days)
        // addWD(Jun1, 3) = Jun4 (Jun2=1, Jun3=2, Jun4=3 — Jun 4 is Thu)
        LocalDate pEf = add(PROJECT_START, 3); // Jun 4 Thursday
        assertThat(pOut.getEsDate()).isEqualTo(PROJECT_START);
        assertThat(pOut.getEfDate()).isEqualTo(pEf);

        // S.ES = P.EF (FS, lag=0)
        assertThat(sOut.getEsDate()).isEqualTo(pEf);
        assertThat(sOut.getEfDate()).isEqualTo(add(pEf, 2));
    }

    // ── SS ────────────────────────────────────────────────────────────────────

    /**
     * SS: Successor can start as soon as predecessor starts (with lag=0).
     * P(3wd) →[SS]→ S(2wd): S.ES = P.ES, S.EF = P.ES + 2wd.
     */
    @Test
    void ss_succStartsWhenPredStarts() {
        CustomerProject p = newProject();
        Task predTask = task(p, "P", PROJECT_START, 3);
        Task succTask = task(p, "S", PROJECT_START, 2);
        edge(succTask, predTask, DependencyType.SS);

        cpm.recompute(p.getId());

        Task pOut = tasks.findById(predTask.getId()).orElseThrow();
        Task sOut = tasks.findById(succTask.getId()).orElseThrow();

        // P: ES = projectStart
        assertThat(pOut.getEsDate()).isEqualTo(PROJECT_START);

        // S.ES = P.ES (SS, lag=0)
        assertThat(sOut.getEsDate()).isEqualTo(PROJECT_START);
        assertThat(sOut.getEfDate()).isEqualTo(add(PROJECT_START, 2));
    }

    /**
     * SS backward pass: P.LF constrains successor start, so
     * P.LF = addWD(S.LS, P.dur) in the SS formula.
     * With S.LF = S.EF (leaf, critical), S.LS = subWD(S.EF, S.dur) = S.ES = P.ES.
     * P.LF = addWD(S.LS, P.dur) = addWD(P.ES, P.dur) = P.EF → P is on the critical path.
     */
    @Test
    void ss_backwardPass_predLfConstrainedBySucStart() {
        CustomerProject p = newProject();
        Task predTask = task(p, "P", PROJECT_START, 3);
        Task succTask = task(p, "S", PROJECT_START, 2);
        edge(succTask, predTask, DependencyType.SS);

        cpm.recompute(p.getId());

        Task pOut = tasks.findById(predTask.getId()).orElseThrow();
        Task sOut = tasks.findById(succTask.getId()).orElseThrow();

        // In an SS chain where P is longer (3wd) than S (2wd):
        // projectFinish = max(P.EF, S.EF) = P.EF = add(Jun1, 3) = Jun4.
        // P is leaf of backward pass (no successors → P.LF = projectFinish).
        // Actually P has a successor (S), so P.LF = addWD(subWD(S.LS, 0), P.dur) [SS backward formula].
        // S.LF = projectFinish (S has no successors).
        // S.LS = subWD(S.LF, 2).
        // P.LF (SS) = addWD(S.LS, P.dur) = addWD(subWD(S.LF, 2), 3).
        LocalDate sLf = add(PROJECT_START, 3); // projectFinish = P.EF = add(Jun1,3)
        LocalDate sLs = sub(sLf, 2);
        LocalDate pLfExpected = add(sLs, 3); // SS backward: addWD(S.LS, P.dur)
        LocalDate pLsExpected = sub(pLfExpected, 3);

        assertThat(pOut.getLfDate()).isEqualTo(pLfExpected);
        assertThat(pOut.getLsDate()).isEqualTo(pLsExpected);
    }

    // ── FF ────────────────────────────────────────────────────────────────────

    /**
     * FF: Successor cannot finish before predecessor finishes.
     * P(3wd) →[FF]→ S(2wd): S.EF = P.EF, so S.ES = P.EF - S.dur = P.EF - 2wd.
     */
    @Test
    void ff_succFinishesWithPred() {
        CustomerProject p = newProject();
        Task predTask = task(p, "P", PROJECT_START, 3);
        Task succTask = task(p, "S", PROJECT_START, 2);
        edge(succTask, predTask, DependencyType.FF);

        cpm.recompute(p.getId());

        Task pOut = tasks.findById(predTask.getId()).orElseThrow();
        Task sOut = tasks.findById(succTask.getId()).orElseThrow();

        LocalDate pEf = add(PROJECT_START, 3);
        assertThat(pOut.getEfDate()).isEqualTo(pEf);

        // S.ES = subWD(P.EF, S.dur) = subWD(pEf, 2)
        LocalDate sEs = sub(pEf, 2);
        assertThat(sOut.getEsDate()).isEqualTo(sEs);
        // S.EF = addWD(S.ES, 2)
        assertThat(sOut.getEfDate()).isEqualTo(add(sEs, 2));
    }

    /**
     * FF backward pass: P.LF = subWD(S.LF, lag=0) = S.LF.
     * With S.LF = S.EF (leaf), and P.LF = S.LF → P.LS = subWD(P.LF, P.dur).
     */
    @Test
    void ff_backwardPass_predLfEqualsSucLf() {
        CustomerProject p = newProject();
        Task predTask = task(p, "P", PROJECT_START, 3);
        Task succTask = task(p, "S", PROJECT_START, 2);
        edge(succTask, predTask, DependencyType.FF);

        cpm.recompute(p.getId());

        Task pOut = tasks.findById(predTask.getId()).orElseThrow();
        Task sOut = tasks.findById(succTask.getId()).orElseThrow();

        // projectFinish = max(P.EF, S.EF). P.EF = add(Jun1,3). S.EF = add(subWD(P.EF,2),2) = P.EF.
        // So projectFinish = P.EF. S is a leaf → S.LF = projectFinish = P.EF.
        // FF backward: P.LF = subWD(S.LF, 0) = S.LF.
        assertThat(pOut.getLfDate()).isEqualTo(sOut.getLfDate());
        // P.LS = subWD(P.LF, 3)
        assertThat(pOut.getLsDate()).isEqualTo(sub(pOut.getLfDate(), 3));
    }

    // ── SF ────────────────────────────────────────────────────────────────────

    /**
     * SF: Successor cannot finish until predecessor starts.
     * P(3wd) →[SF]→ S(2wd), lag=0: S.EF = P.ES, so S.ES = P.ES - S.dur.
     * P.ES = projectStart = Jun1.
     * S.ES = subWD(Jun1, 2) = May29 (Fri).
     * S.EF = addWD(May29, 2) = Jun1.
     *
     * projectStart = Jun1, but S starts before projectStart.
     * The engine's forward-pass formula gives S.ES = subWD(P.ES, S.dur).
     * The "max(projectStart, ...)" clamp in the forward pass must NOT be applied
     * to the edge-level candidate before comparing; the clamp uses max across
     * all incoming edge bounds. So if S.ES from SF formula < projectStart,
     * and there are no other predecessors pushing it later, S.ES can be < projectStart.
     * (This is valid CPM — the successor's constrained-finish was already in the past.)
     */
    @Test
    void sf_succFinishesWhenPredStarts() {
        CustomerProject p = newProject();
        Task predTask = task(p, "P", PROJECT_START, 3);
        Task succTask = task(p, "S", PROJECT_START, 2);
        edge(succTask, predTask, DependencyType.SF);

        cpm.recompute(p.getId());

        Task pOut = tasks.findById(predTask.getId()).orElseThrow();
        Task sOut = tasks.findById(succTask.getId()).orElseThrow();

        // P.ES = projectStart = Jun1 (source node)
        assertThat(pOut.getEsDate()).isEqualTo(PROJECT_START);

        // SF forward: S.ES candidate = subWD(P.ES, S.dur) = subWD(Jun1, 2) = May 29 (Fri)
        LocalDate sEsCandidate = sub(PROJECT_START, 2);
        // max(projectStart=Jun1, sEsCandidate=May29) = Jun1
        // So the final S.ES = max(projectStart, candidate) = Jun1
        // And S.EF = addWD(Jun1, 2)
        LocalDate sEs = sEsCandidate.isBefore(PROJECT_START) ? PROJECT_START : sEsCandidate;
        assertThat(sOut.getEsDate()).isEqualTo(sEs);
        assertThat(sOut.getEfDate()).isEqualTo(add(sEs, 2));
    }

    /**
     * SF backward pass: P.LF = addWD(subWD(S.LF, lag=0), P.dur).
     */
    @Test
    void sf_backwardPass_predLfFromSucLf() {
        CustomerProject p = newProject();
        Task predTask = task(p, "P", PROJECT_START, 3);
        Task succTask = task(p, "S", PROJECT_START, 2);
        edge(succTask, predTask, DependencyType.SF);

        cpm.recompute(p.getId());

        Task pOut = tasks.findById(predTask.getId()).orElseThrow();
        Task sOut = tasks.findById(succTask.getId()).orElseThrow();

        // S.LF = projectFinish (S is a leaf).
        // SF backward: P.LF = addWD(subWD(S.LF, 0), P.dur) = addWD(S.LF, P.dur)
        LocalDate pLfExpected = add(sOut.getLfDate(), 3);
        assertThat(pOut.getLfDate()).isEqualTo(pLfExpected);
    }

    // ── 3-node chains ─────────────────────────────────────────────────────────

    /**
     * 3-node SS chain: A(3wd) →[SS]→ B(3wd) →[SS]→ C(2wd).
     * All three start together (SS, lag=0). C.ES = B.ES = A.ES = projectStart.
     */
    @Test
    void ss_3nodeChain_allStartTogether() {
        CustomerProject p = newProject();
        Task a = task(p, "A", PROJECT_START, 3);
        Task b = task(p, "B", PROJECT_START, 3);
        Task c = task(p, "C", PROJECT_START, 2);
        edge(b, a, DependencyType.SS);
        edge(c, b, DependencyType.SS);

        cpm.recompute(p.getId());

        Task aOut = tasks.findById(a.getId()).orElseThrow();
        Task bOut = tasks.findById(b.getId()).orElseThrow();
        Task cOut = tasks.findById(c.getId()).orElseThrow();

        assertThat(aOut.getEsDate()).isEqualTo(PROJECT_START);
        assertThat(bOut.getEsDate()).isEqualTo(PROJECT_START);
        assertThat(cOut.getEsDate()).isEqualTo(PROJECT_START);
    }

    /**
     * 3-node FF chain: A(3wd) →[FF]→ B(3wd) →[FF]→ C(2wd).
     * All three finish together. C.EF = B.EF = A.EF = projectStart + 3wd.
     */
    @Test
    void ff_3nodeChain_allFinishTogether() {
        CustomerProject p = newProject();
        Task a = task(p, "A", PROJECT_START, 3);
        Task b = task(p, "B", PROJECT_START, 3);
        Task c = task(p, "C", PROJECT_START, 2);
        edge(b, a, DependencyType.FF);
        edge(c, b, DependencyType.FF);

        cpm.recompute(p.getId());

        Task aOut = tasks.findById(a.getId()).orElseThrow();
        Task bOut = tasks.findById(b.getId()).orElseThrow();
        Task cOut = tasks.findById(c.getId()).orElseThrow();

        LocalDate expectedFinish = add(PROJECT_START, 3);
        assertThat(aOut.getEfDate()).isEqualTo(expectedFinish);
        assertThat(bOut.getEfDate()).isEqualTo(expectedFinish);
        assertThat(cOut.getEfDate()).isEqualTo(expectedFinish);
    }

    /**
     * Mixed-type chain: A(3wd) →[FS]→ B(2wd) →[SS]→ C(4wd).
     * A.EF = add(Jun1, 3). B.ES = A.EF. B.EF = add(B.ES, 2).
     * C.ES = B.ES (SS). C.EF = add(C.ES, 4).
     */
    @Test
    void mixed_fs_then_ss() {
        CustomerProject p = newProject();
        Task a = task(p, "A", PROJECT_START, 3);
        Task b = task(p, "B", PROJECT_START, 2);
        Task c = task(p, "C", PROJECT_START, 4);
        edge(b, a, DependencyType.FS);
        edge(c, b, DependencyType.SS);

        cpm.recompute(p.getId());

        Task aOut = tasks.findById(a.getId()).orElseThrow();
        Task bOut = tasks.findById(b.getId()).orElseThrow();
        Task cOut = tasks.findById(c.getId()).orElseThrow();

        LocalDate aEf = add(PROJECT_START, 3);
        LocalDate bEs = aEf;         // FS
        LocalDate bEf = add(bEs, 2);
        LocalDate cEs = bEs;         // SS: C.ES = B.ES
        LocalDate cEf = add(cEs, 4);

        assertThat(aOut.getEfDate()).isEqualTo(aEf);
        assertThat(bOut.getEsDate()).isEqualTo(bEs);
        assertThat(bOut.getEfDate()).isEqualTo(bEf);
        assertThat(cOut.getEsDate()).isEqualTo(cEs);
        assertThat(cOut.getEfDate()).isEqualTo(cEf);
    }

    /**
     * FS-regression: all-FS graph yields identical ES/EF to what the original
     * single-type engine produced. This is the canonical backward-compatibility check.
     */
    @Test
    void fs_allEdges_regressionMatchesBaseline() {
        CustomerProject p = newProject();
        Task a = task(p, "A", PROJECT_START, 4);
        Task b = task(p, "B", PROJECT_START, 5);
        Task c = task(p, "C", PROJECT_START, 8);
        Task d = task(p, "D", PROJECT_START, 2);
        edge(b, a, DependencyType.FS);
        edge(c, a, DependencyType.FS);
        edge(d, b, DependencyType.FS);
        edge(d, c, DependencyType.FS);

        cpm.recompute(p.getId());

        Task aOut = tasks.findById(a.getId()).orElseThrow();
        Task bOut = tasks.findById(b.getId()).orElseThrow();
        Task cOut = tasks.findById(c.getId()).orElseThrow();
        Task dOut = tasks.findById(d.getId()).orElseThrow();

        // A: source
        LocalDate aEf = add(PROJECT_START, 4);
        assertThat(aOut.getEsDate()).isEqualTo(PROJECT_START);
        assertThat(aOut.getEfDate()).isEqualTo(aEf);

        // B: A→B(FS), 5wd
        assertThat(bOut.getEsDate()).isEqualTo(aEf);
        assertThat(bOut.getEfDate()).isEqualTo(add(aEf, 5));

        // C: A→C(FS), 8wd
        assertThat(cOut.getEsDate()).isEqualTo(aEf);
        assertThat(cOut.getEfDate()).isEqualTo(add(aEf, 8));

        // D: max(B.EF, C.EF) → C-branch is longer (8 > 5)
        LocalDate cEf = add(aEf, 8);
        assertThat(dOut.getEsDate()).isEqualTo(cEf);
        assertThat(dOut.getEfDate()).isEqualTo(add(cEf, 2));

        // Float: C-path is critical; B has float = 8 - 5 = 3
        assertThat(cOut.getIsCritical()).isTrue();
        assertThat(bOut.getTotalFloatDays()).isEqualTo(3);
    }
}
