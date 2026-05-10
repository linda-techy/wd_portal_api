package com.wd.api.service.scheduling;

import com.wd.api.model.enums.FloorLoop;
import com.wd.api.model.scheduling.WbsTemplate;
import com.wd.api.model.scheduling.WbsTemplatePhase;
import com.wd.api.model.scheduling.WbsTemplateTask;
import com.wd.api.model.scheduling.WbsTemplateTaskPredecessor;
import com.wd.api.repository.scheduling.WbsTemplatePhaseRepository;
import com.wd.api.repository.scheduling.WbsTemplateRepository;
import com.wd.api.repository.scheduling.WbsTemplateTaskPredecessorRepository;
import com.wd.api.repository.scheduling.WbsTemplateTaskRepository;
import com.wd.api.service.scheduling.dto.WbsTemplateDto;
import com.wd.api.service.scheduling.dto.WbsTemplatePhaseDto;
import com.wd.api.service.scheduling.dto.WbsTemplateTaskDto;
import com.wd.api.service.scheduling.dto.WbsTemplateTaskPredecessorDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WBS template authoring service. Implements versioning semantics:
 *
 * <ul>
 *   <li>{@link #create} creates version 1 and marks it active.</li>
 *   <li>{@link #update} flips the previous version's {@code isActive=false}
 *       and inserts a new row with version=max+1, isActive=true. The previous
 *       row is preserved so existing project clones (snapshot) remain
 *       referenceable for audit.</li>
 *   <li>{@link #deactivate} only flips the active flag — soft delete.</li>
 * </ul>
 */
@Service
public class WbsTemplateService {

    private final WbsTemplateRepository templates;
    private final WbsTemplatePhaseRepository phases;
    private final WbsTemplateTaskRepository tasks;
    private final WbsTemplateTaskPredecessorRepository preds;

    public WbsTemplateService(WbsTemplateRepository templates,
                              WbsTemplatePhaseRepository phases,
                              WbsTemplateTaskRepository tasks,
                              WbsTemplateTaskPredecessorRepository preds) {
        this.templates = templates;
        this.phases = phases;
        this.tasks = tasks;
        this.preds = preds;
    }

    @Transactional(readOnly = true)
    public List<WbsTemplateDto> list(boolean includeInactive) {
        List<WbsTemplate> rows = includeInactive ? templates.findAll() : templates.findByIsActiveTrue();
        // Don't descend into phases/tasks/predecessors for the list view —
        // toDto() does N×M×K queries (phases per template, tasks per phase,
        // predecessors per task) which puts the small-dataset response time
        // in the ~15s range. The list endpoint only needs the template
        // header. Callers that need the full hierarchy use get() / getActiveByCode().
        // Surfaced via the portal-features Playwright suite (Tier 6).
        return rows.stream().map(this::toListDto).toList();
    }

    /** Lightweight list-view projection — no child loading. */
    private WbsTemplateDto toListDto(WbsTemplate t) {
        return new WbsTemplateDto(t.getId(), t.getCode(), t.getProjectType(), t.getName(),
                t.getDescription(), t.getVersion(), t.getIsActive(),
                java.util.List.of(), t.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public WbsTemplateDto get(Long id) {
        return templates.findById(id).map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("WbsTemplate " + id));
    }

    @Transactional(readOnly = true)
    public WbsTemplateDto getActiveByCode(String code) {
        return templates.findByCodeAndIsActiveTrue(code).map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("active template " + code));
    }

    @Transactional(readOnly = true)
    public WbsTemplateDto getByCodeAndVersion(String code, Integer version) {
        return templates.findByCodeAndVersion(code, version).map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException(
                        "template " + code + " v" + version));
    }

    @Transactional
    public WbsTemplateDto create(WbsTemplateDto req) {
        WbsTemplate t = new WbsTemplate();
        t.setCode(req.code());
        t.setProjectType(req.projectType());
        t.setName(req.name());
        t.setDescription(req.description());
        t.setVersion(1);
        t.setIsActive(Boolean.TRUE);
        t = templates.save(t);
        persistChildren(t, req);
        return toDto(t);
    }

    /**
     * Versioned save. Marks the existing row inactive and inserts a new row
     * with version = previous-max + 1, isActive=true. Snapshot semantics:
     * existing project clones keep referencing the historical row's content
     * (they don't store an FK, so this is purely about which row is "active"
     * for future clones).
     */
    @Transactional
    public WbsTemplateDto update(Long existingId, WbsTemplateDto req) {
        WbsTemplate prev = templates.findById(existingId)
                .orElseThrow(() -> new EntityNotFoundException("WbsTemplate " + existingId));
        int nextVersion = templates.findMaxVersionForCode(prev.getCode()).orElse(0) + 1;

        // The partial unique index uk_wbs_template_one_active_per_code (V116)
        // enforces "at most one is_active=TRUE row per code". We deactivate the
        // existing active version and flush BEFORE inserting the new active one
        // to avoid the index conflict within a single transaction.
        prev.setIsActive(Boolean.FALSE);
        templates.save(prev);
        templates.flush();

        WbsTemplate next = new WbsTemplate();
        next.setCode(prev.getCode());
        next.setProjectType(req.projectType() != null ? req.projectType() : prev.getProjectType());
        next.setName(req.name());
        next.setDescription(req.description());
        next.setVersion(nextVersion);
        next.setIsActive(Boolean.TRUE);
        next = templates.save(next);
        persistChildren(next, req);
        return toDto(next);
    }

    @Transactional
    public void deactivate(Long id) {
        WbsTemplate t = templates.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WbsTemplate " + id));
        t.setIsActive(Boolean.FALSE);
        templates.save(t);
    }

    // ---- helpers ----

    private void persistChildren(WbsTemplate template, WbsTemplateDto req) {
        if (req.phases() == null) return;
        Map<Long, WbsTemplateTask> taskByDtoId = new HashMap<>();
        for (WbsTemplatePhaseDto phaseDto : req.phases()) {
            WbsTemplatePhase phase = new WbsTemplatePhase();
            phase.setTemplate(template);
            phase.setSequence(phaseDto.sequence());
            phase.setName(phaseDto.name());
            phase.setRoleHint(phaseDto.roleHint());
            phase.setMonsoonSensitive(Boolean.TRUE.equals(phaseDto.monsoonSensitive()));
            phase = phases.save(phase);
            if (phaseDto.tasks() != null) {
                for (WbsTemplateTaskDto td : phaseDto.tasks()) {
                    WbsTemplateTask task = toTaskEntity(td, phase);
                    task = tasks.save(task);
                    if (td.id() != null) taskByDtoId.put(td.id(), task);
                }
            }
        }
        for (WbsTemplatePhaseDto phaseDto : req.phases()) {
            if (phaseDto.tasks() == null) continue;
            for (WbsTemplateTaskDto td : phaseDto.tasks()) {
                if (td.predecessors() == null) continue;
                WbsTemplateTask successor = taskByDtoId.get(td.id());
                if (successor == null) continue;
                for (WbsTemplateTaskPredecessorDto pd : td.predecessors()) {
                    WbsTemplateTask pred = taskByDtoId.get(pd.predecessorTaskId());
                    if (pred == null) continue;
                    WbsTemplateTaskPredecessor entity = new WbsTemplateTaskPredecessor();
                    entity.setSuccessor(successor);
                    entity.setPredecessor(pred);
                    entity.setLagDays(pd.lagDays() != null ? pd.lagDays() : 0);
                    entity.setDepType(pd.depType() != null ? pd.depType() : "FS");
                    preds.save(entity);
                }
            }
        }
    }

    private WbsTemplateTask toTaskEntity(WbsTemplateTaskDto td, WbsTemplatePhase phase) {
        WbsTemplateTask t = new WbsTemplateTask();
        t.setPhase(phase);
        t.setSequence(td.sequence());
        t.setName(td.name());
        t.setRoleHint(td.roleHint());
        t.setDurationDays(td.durationDays());
        t.setWeightFactor(td.weightFactor());
        t.setMonsoonSensitive(Boolean.TRUE.equals(td.monsoonSensitive()));
        t.setIsPaymentMilestone(Boolean.TRUE.equals(td.isPaymentMilestone()));
        t.setFloorLoop(td.floorLoop() != null ? td.floorLoop() : FloorLoop.NONE);
        t.setOptionalCost(td.optionalCost());
        return t;
    }

    private WbsTemplateDto toDto(WbsTemplate t) {
        List<WbsTemplatePhaseDto> phaseDtos = phases.findByTemplateIdOrderBySequenceAsc(t.getId())
                .stream().map(p -> {
                    List<WbsTemplateTaskDto> taskDtos = tasks.findByPhaseIdOrderBySequenceAsc(p.getId())
                            .stream().map(this::toTaskDto).toList();
                    return new WbsTemplatePhaseDto(p.getId(), p.getSequence(), p.getName(),
                            p.getRoleHint(), p.getMonsoonSensitive(), taskDtos);
                }).toList();
        return new WbsTemplateDto(t.getId(), t.getCode(), t.getProjectType(), t.getName(),
                t.getDescription(), t.getVersion(), t.getIsActive(), phaseDtos, t.getUpdatedAt());
    }

    private WbsTemplateTaskDto toTaskDto(WbsTemplateTask task) {
        List<WbsTemplateTaskPredecessorDto> ps = preds.findBySuccessorId(task.getId())
                .stream().map(p -> new WbsTemplateTaskPredecessorDto(
                        p.getId(), p.getPredecessor().getId(), p.getLagDays(), p.getDepType()))
                .toList();
        return new WbsTemplateTaskDto(task.getId(), task.getSequence(), task.getName(),
                task.getRoleHint(), task.getDurationDays(), task.getWeightFactor(),
                task.getMonsoonSensitive(), task.getIsPaymentMilestone(),
                task.getFloorLoop(), task.getOptionalCost(), ps);
    }
}
