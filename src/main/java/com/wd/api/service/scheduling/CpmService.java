package com.wd.api.service.scheduling;

import com.wd.api.dto.scheduling.CpmResultDto;
import com.wd.api.dto.scheduling.CpmTaskDto;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.model.scheduling.ProjectScheduleConfig;
import com.wd.api.model.scheduling.TaskPredecessor;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.repository.TaskPredecessorRepository;
import com.wd.api.repository.TaskRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Critical Path Method engine. Given a project's tasks and predecessor edges,
 * computes ES/EF (forward pass) and LS/LF (backward pass), total float, and a
 * critical flag — then persists results denormalized on the {@code tasks} table.
 *
 * <p>Pure-ish: the algorithm itself has no Spring magic; the service shell does
 * I/O (load tasks, load holidays, batch save) inside a single transaction.
 */
@Service
public class CpmService {

    private static final Logger log = LoggerFactory.getLogger(CpmService.class);

    private final TaskRepository taskRepo;
    private final TaskPredecessorRepository predRepo;
    private final CustomerProjectRepository projectRepo;
    private final ProjectScheduleConfigRepository configRepo;
    private final HolidayService holidayService;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @PersistenceContext
    private EntityManager em;

    public CpmService(TaskRepository taskRepo,
                      TaskPredecessorRepository predRepo,
                      CustomerProjectRepository projectRepo,
                      ProjectScheduleConfigRepository configRepo,
                      HolidayService holidayService,
                      JdbcTemplate jdbc,
                      Clock clock) {
        this.taskRepo = taskRepo;
        this.predRepo = predRepo;
        this.projectRepo = projectRepo;
        this.configRepo = configRepo;
        this.holidayService = holidayService;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /**
     * Recompute CPM for a project; persist es/ef/ls/lf/totalFloat/isCritical on
     * every task. Idempotent. O(V+E) where V = task count, E = edge count.
     */
    @Transactional
    public void recompute(Long projectId) {
        long t0 = System.currentTimeMillis();

        List<Task> tasks = taskRepo.findByProjectId(projectId);
        if (tasks.isEmpty()) {
            log.info("CPM recompute: project={} tasks=0 duration={}ms", projectId,
                    System.currentTimeMillis() - t0);
            return;
        }

        Map<Long, Task> byId = new HashMap<>();
        for (Task t : tasks) byId.put(t.getId(), t);

        // Build edges in both directions for traversal. Fetch all in one query
        // (avoids N+1 across the task list).
        List<Long> taskIds = new ArrayList<>(tasks.size());
        for (Task t : tasks) taskIds.add(t.getId());
        List<TaskPredecessor> edges = taskIds.isEmpty()
                ? List.of()
                : predRepo.findBySuccessorIdIn(taskIds);

        Map<Long, List<TaskPredecessor>> incoming = new HashMap<>();   // successor -> its preds
        Map<Long, List<TaskPredecessor>> outgoing = new HashMap<>();   // predecessor -> its succs
        for (TaskPredecessor e : edges) {
            if (!byId.containsKey(e.getSuccessorId()) || !byId.containsKey(e.getPredecessorId())) continue;
            incoming.computeIfAbsent(e.getSuccessorId(), k -> new ArrayList<>()).add(e);
            outgoing.computeIfAbsent(e.getPredecessorId(), k -> new ArrayList<>()).add(e);
        }

        // Project start fallback: CustomerProject.startDate -> first task.startDate -> today.
        LocalDate projectStart = resolveProjectStart(projectId, tasks);

        // Holiday set + sundayWorking lookup.
        boolean sundayWorking = configRepo.findByProjectId(projectId)
                .map(ProjectScheduleConfig::getSundayWorking).orElse(false);
        // Window for holiday lookup: project start to start + 5 years (broad enough
        // to cover any sane construction timeline without a second pass).
        Set<LocalDate> holidays = holidayService.holidaysFor(
                projectId, projectStart, projectStart.plusYears(5));

        List<Long> order = topologicalOrder(tasks, incoming);

        // ---- Forward pass ----
        for (Long id : order) {
            Task t = byId.get(id);
            int duration = durationDays(t);
            List<TaskPredecessor> in = incoming.getOrDefault(id, List.of());

            if (t.getActualEndDate() != null) {
                // Completed: anchor on actual end.
                t.setEfDate(t.getActualEndDate());
                if (t.getActualStartDate() != null) {
                    t.setEsDate(t.getActualStartDate());
                } else {
                    t.setEsDate(WorkingDayCalculator.subtractWorkingDays(
                            t.getActualEndDate(), duration, holidays, sundayWorking));
                }
            } else if (t.getActualStartDate() != null) {
                // In progress: ES anchored, EF projected from today + remaining days.
                LocalDate today = LocalDate.now(clock);
                t.setEsDate(t.getActualStartDate());
                int worked = (today.isBefore(t.getActualStartDate()))
                        ? 0
                        : WorkingDayCalculator.workingDaysBetween(
                                t.getActualStartDate(), today, holidays, sundayWorking);
                int remaining = Math.max(0, duration - worked);
                t.setEfDate(WorkingDayCalculator.addWorkingDays(
                        today.isBefore(t.getActualStartDate()) ? t.getActualStartDate() : today,
                        remaining, holidays, sundayWorking));
            } else {
                // Planned: ES = max(predecessor EF + lag) or projectStart if leaf-source.
                LocalDate es = projectStart;
                for (TaskPredecessor edge : in) {
                    Task predTask = byId.get(edge.getPredecessorId());
                    LocalDate candidate = WorkingDayCalculator.addWorkingDays(
                            predTask.getEfDate(), edge.getLagDays() == null ? 0 : edge.getLagDays(),
                            holidays, sundayWorking);
                    if (candidate.isAfter(es)) es = candidate;
                }
                t.setEsDate(es);
                t.setEfDate(WorkingDayCalculator.addWorkingDays(es, duration, holidays, sundayWorking));
            }
        }

        // ---- Backward pass ----
        LocalDate projectFinish = byId.values().stream()
                .map(Task::getEfDate)
                .filter(d -> d != null)
                .max(Comparator.naturalOrder())
                .orElse(projectStart);

        // Reverse topological order.
        List<Long> reverse = new ArrayList<>(order);
        java.util.Collections.reverse(reverse);
        for (Long id : reverse) {
            Task t = byId.get(id);
            int duration = durationDays(t);
            List<TaskPredecessor> out = outgoing.getOrDefault(id, List.of());
            LocalDate lf;
            if (out.isEmpty()) {
                lf = projectFinish;
            } else {
                lf = null;
                for (TaskPredecessor edge : out) {
                    Task succ = byId.get(edge.getSuccessorId());
                    int lag = edge.getLagDays() == null ? 0 : edge.getLagDays();
                    LocalDate candidate = WorkingDayCalculator.subtractWorkingDays(
                            succ.getLsDate(), lag, holidays, sundayWorking);
                    if (lf == null || candidate.isBefore(lf)) lf = candidate;
                }
            }
            t.setLfDate(lf);
            t.setLsDate(WorkingDayCalculator.subtractWorkingDays(lf, duration, holidays, sundayWorking));
        }

        // ---- Float + critical flag ----
        // CPM theory permits negative total float — it's the canonical signal
        // of a task that has slipped past its latest-allowable date. Compute
        // symmetrically: workingDaysBetween throws when end<start, so flip the
        // bounds and negate when ls < es. Treat negative-float tasks as
        // critical (floatDays <= 0), matching PM-tooling convention.
        for (Task t : byId.values()) {
            int floatDays;
            if (t.getLsDate().isBefore(t.getEsDate())) {
                floatDays = -WorkingDayCalculator.workingDaysBetween(
                        t.getLsDate(), t.getEsDate(), holidays, sundayWorking);
            } else {
                floatDays = WorkingDayCalculator.workingDaysBetween(
                        t.getEsDate(), t.getLsDate(), holidays, sundayWorking);
            }
            t.setTotalFloatDays(floatDays);
            t.setIsCritical(floatDays <= 0);
        }

        // ---- Persist (single batched UPDATE; bypasses JPA per-row roundtrips) ----
        // Detach the JPA-managed entities BEFORE issuing the JDBC batch so that
        // (a) JPA does not auto-flush its dirty-checked UPDATEs on top of ours
        //     (the entity instances are dirty from our in-memory mutations), and
        // (b) downstream reads in the same transaction reload from DB and
        //     see the new column values.
        List<Task> persistOrder = new ArrayList<>(byId.values());
        for (Task t : persistOrder) em.detach(t);
        jdbc.batchUpdate(
                "UPDATE tasks SET es_date = ?, ef_date = ?, ls_date = ?, lf_date = ?, " +
                        "total_float_days = ?, is_critical = ?, " +
                        "actual_start_date = ?, actual_end_date = ? " +
                        "WHERE id = ?",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Task t = persistOrder.get(i);
                        setDate(ps, 1, t.getEsDate());
                        setDate(ps, 2, t.getEfDate());
                        setDate(ps, 3, t.getLsDate());
                        setDate(ps, 4, t.getLfDate());
                        if (t.getTotalFloatDays() == null) ps.setNull(5, Types.INTEGER);
                        else ps.setInt(5, t.getTotalFloatDays());
                        ps.setBoolean(6, Boolean.TRUE.equals(t.getIsCritical()));
                        setDate(ps, 7, t.getActualStartDate());
                        setDate(ps, 8, t.getActualEndDate());
                        ps.setLong(9, t.getId());
                    }

                    @Override
                    public int getBatchSize() {
                        return persistOrder.size();
                    }
                });

        long durMs = System.currentTimeMillis() - t0;
        log.info("CPM recompute: project={} tasks={} duration={}ms",
                projectId, tasks.size(), durMs);

        LocalDate horizon = WorkingDayCalculator.addWorkingDays(
                projectStart, 365, holidays, sundayWorking);
        for (Task t : byId.values()) {
            if (t.getEfDate() != null && t.getEfDate().isAfter(horizon)) {
                log.warn("CPM recompute: project={} task={} efDate={} exceeds 365-working-day horizon — likely a duration data-entry error",
                        projectId, t.getId(), t.getEfDate());
                break;
            }
        }
    }

    /**
     * Read CPM result snapshot for a project. Does NOT recompute — assumes
     * persisted columns are current.
     */
    @Transactional(readOnly = true)
    public CpmResultDto read(Long projectId) {
        List<Task> tasks = taskRepo.findByProjectId(projectId);
        LocalDate projectStart = resolveProjectStart(projectId, tasks);
        LocalDate finish = tasks.stream()
                .map(Task::getEfDate)
                .filter(d -> d != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
        List<Long> critical = new ArrayList<>();
        List<CpmTaskDto> rows = new ArrayList<>(tasks.size());
        for (Task t : tasks) {
            boolean isCrit = Boolean.TRUE.equals(t.getIsCritical());
            if (isCrit) critical.add(t.getId());
            rows.add(new CpmTaskDto(
                    t.getId(),
                    t.getTitle(),
                    durationDays(t),
                    t.getEsDate(),
                    t.getEfDate(),
                    t.getLsDate(),
                    t.getLfDate(),
                    t.getTotalFloatDays(),
                    isCrit));
        }
        return new CpmResultDto(projectId, projectStart, finish, critical, rows);
    }

    // ─── helpers ──────────────────────────────────────────────────────────

    /** Bind a nullable LocalDate to a JDBC parameter. */
    private static void setDate(PreparedStatement ps, int idx, LocalDate value) throws SQLException {
        if (value == null) ps.setNull(idx, Types.DATE);
        else ps.setDate(idx, Date.valueOf(value));
    }

    /** Working-day duration derived from start/end. Floors at 0; never negative. */
    private static int durationDays(Task t) {
        if (t.getStartDate() == null || t.getEndDate() == null) return 0;
        if (t.getEndDate().isBefore(t.getStartDate())) return 0;
        return WorkingDayCalculator.workingDaysBetween(
                t.getStartDate(), t.getEndDate(), java.util.Set.of(), false);
    }

    private LocalDate resolveProjectStart(Long projectId, List<Task> tasks) {
        Optional<CustomerProject> proj = projectRepo.findById(projectId);
        if (proj.isPresent() && proj.get().getStartDate() != null) {
            return proj.get().getStartDate();
        }
        return tasks.stream()
                .map(Task::getStartDate)
                .filter(d -> d != null)
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now(clock));
    }

    /**
     * Kahn's-algorithm topological sort. Defensive: throws if a cycle remains
     * (S1's TaskGraphValidator should already prevent this on writes).
     */
    private List<Long> topologicalOrder(List<Task> tasks, Map<Long, List<TaskPredecessor>> incoming) {
        Map<Long, Integer> indeg = new HashMap<>();
        for (Task t : tasks) indeg.put(t.getId(), incoming.getOrDefault(t.getId(), List.of()).size());

        Deque<Long> queue = new ArrayDeque<>();
        for (Map.Entry<Long, Integer> e : indeg.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }

        // Reverse adjacency from `incoming`: pred -> succs.
        Map<Long, List<Long>> succsOf = new HashMap<>();
        for (Map.Entry<Long, List<TaskPredecessor>> e : incoming.entrySet()) {
            for (TaskPredecessor edge : e.getValue()) {
                succsOf.computeIfAbsent(edge.getPredecessorId(), k -> new ArrayList<>())
                        .add(edge.getSuccessorId());
            }
        }

        List<Long> order = new ArrayList<>(tasks.size());
        Set<Long> seen = new HashSet<>();
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            if (!seen.add(id)) continue;
            order.add(id);
            for (Long s : succsOf.getOrDefault(id, List.of())) {
                int d = indeg.getOrDefault(s, 0) - 1;
                indeg.put(s, d);
                if (d == 0) queue.add(s);
            }
        }
        if (order.size() != tasks.size()) {
            throw new IllegalStateException(
                    "CPM cycle detected in project tasks; topological sort produced "
                    + order.size() + " of " + tasks.size() + " — should be unreachable post-S1");
        }
        return order;
    }
}
