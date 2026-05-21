package com.wd.api.service;

import com.wd.api.exception.ResourceNotFoundException;
import com.wd.api.model.PortalUser;
import com.wd.api.model.Task;
import com.wd.api.model.TaskQualityGate;
import com.wd.api.model.TaskQualityGate.GateType;
import com.wd.api.model.TaskQualityGate.Status;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.TaskQualityGateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Manages ITP (Inspection-Test Plan) quality gates per task.
 *
 * Lifecycle:
 *   - 3 PENDING gates (PRELIMINARY, IN_PROGRESS, FINAL) are created with every
 *     new task (called from TaskService.createTask).
 *   - Each gate is signed sequentially: you cannot sign IN_PROGRESS until
 *     PRELIMINARY has PASSED; you cannot sign FINAL until IN_PROGRESS has PASSED.
 *   - A task cannot be marked COMPLETED until FINAL has PASSED — enforced by
 *     {@link #assertCompletable(Long)} called from the task-completion path.
 */
@Service
@Transactional
public class TaskQualityGateService {

    private static final Logger logger = LoggerFactory.getLogger(TaskQualityGateService.class);

    /**
     * Role codes allowed to sign off a gate on someone ELSE's task — the
     * "supervisory override" path. Frontline engineers (SITE_ENGINEER,
     * SITE_SUPERVISOR, MEP_SUPERVISOR, FOREMAN) can only sign their OWN
     * assigned tasks; anyone listed here can sign on behalf when needed.
     */
    private static final Set<String> SUPERVISORY_ROLE_CODES = Set.of(
            "ADMIN", "PROJECT_MANAGER", "QUALITY_SAFETY");

    private final TaskQualityGateRepository gateRepository;
    private final PortalUserRepository portalUserRepository;

    public TaskQualityGateService(TaskQualityGateRepository gateRepository,
                                  PortalUserRepository portalUserRepository) {
        this.gateRepository = gateRepository;
        this.portalUserRepository = portalUserRepository;
    }

    /** Create the 3 PENDING gates for a freshly-saved task. Idempotent. */
    public void seedGatesFor(Task task) {
        for (GateType type : GateType.values()) {
            if (gateRepository.findByTaskIdAndGateType(task.getId(), type).isPresent()) {
                continue;
            }
            TaskQualityGate g = new TaskQualityGate();
            g.setTask(task);
            g.setGateType(type);
            g.setStatus(Status.PENDING);
            gateRepository.save(g);
        }
    }

    @Transactional(readOnly = true)
    public List<TaskQualityGate> getGates(Long taskId) {
        return gateRepository.findByTaskId(taskId);
    }

    /**
     * Sign off a gate. Enforces:
     *   - newStatus is one of PASSED / FAILED / NA (PENDING is the initial state)
     *   - the previous gate (if any) must be PASSED or NA before this one can be signed
     *   - FAILED requires a failureReason
     *   - the gate's signed-by + signed-at + notes are stamped on the row
     */
    public TaskQualityGate signOff(Long taskId,
                                    GateType type,
                                    Status newStatus,
                                    String notes,
                                    String failureReason,
                                    Long signedByUserId) {
        if (newStatus == Status.PENDING) {
            throw new IllegalArgumentException("Cannot sign off with status PENDING; use PASSED, FAILED, or NA.");
        }
        if (newStatus == Status.FAILED && (failureReason == null || failureReason.isBlank())) {
            throw new IllegalArgumentException("A failure reason is required when failing a gate.");
        }

        TaskQualityGate gate = gateRepository.findByTaskIdAndGateType(taskId, type)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Gate not found for task " + taskId + " / type " + type));

        // Sequencing rule: PRELIMINARY → IN_PROGRESS → FINAL.
        if (type == GateType.IN_PROGRESS) {
            assertPriorPassed(taskId, GateType.PRELIMINARY,
                "Sign off PRELIMINARY before signing IN_PROGRESS.");
        } else if (type == GateType.FINAL) {
            assertPriorPassed(taskId, GateType.PRELIMINARY,
                "Sign off PRELIMINARY before signing FINAL.");
            assertPriorPassed(taskId, GateType.IN_PROGRESS,
                "Sign off IN_PROGRESS before signing FINAL.");
        }

        PortalUser signer = portalUserRepository.findById(signedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Signing user not found: " + signedByUserId));

        // Accountability rule: the assigned site engineer signs their own
        // gates. A supervisor (PM, ADMIN, QUALITY_SAFETY) may override when
        // the assignee is unavailable. Anyone else with TASK_QC_SIGNOFF is
        // rejected here even though they passed the @PreAuthorize check.
        Task task = gate.getTask();
        PortalUser assignee = task != null ? task.getAssignedTo() : null;
        if (assignee != null && !assignee.getId().equals(signedByUserId)
                && !hasSupervisoryRole(signer)) {
            throw new AccessDeniedException(
                "Only the assigned site engineer ("
                    + (assignee.getFirstName() == null ? "" : assignee.getFirstName())
                    + " " + (assignee.getLastName() == null ? "" : assignee.getLastName()).trim()
                    + ") may sign off this gate. Project managers, admins, or QA/safety can override.");
        }

        gate.setStatus(newStatus);
        gate.setSignedBy(signer);
        gate.setSignedAt(LocalDateTime.now());
        gate.setNotes(notes);
        gate.setFailureReason(newStatus == Status.FAILED ? failureReason : null);
        TaskQualityGate saved = gateRepository.save(gate);

        logger.info("Task {} gate {} signed as {} by user {} (assignee={}, supervisorOverride={})",
                taskId, type, newStatus, signedByUserId,
                assignee != null ? assignee.getId() : null,
                assignee != null && !assignee.getId().equals(signedByUserId));
        return saved;
    }

    private boolean hasSupervisoryRole(PortalUser user) {
        if (user == null || user.getRole() == null) return false;
        String code = user.getRole().getCode();
        if (code == null) return false;
        return SUPERVISORY_ROLE_CODES.contains(code.toUpperCase());
    }

    /**
     * Throws if the task cannot move to COMPLETED because its FINAL gate has
     * not been PASSED (or NA). Call this from the completion path before
     * persisting status=COMPLETED.
     */
    public void assertCompletable(Long taskId) {
        TaskQualityGate finalGate = gateRepository
                .findByTaskIdAndGateType(taskId, GateType.FINAL)
                .orElse(null);
        if (finalGate == null) {
            // Task pre-dates V152 and has no row yet. Be safe: refuse and surface
            // the policy. Callers can re-seed via seedGatesFor() if needed.
            throw new IllegalStateException(
                "FINAL quality gate is missing for this task. Re-seed gates or contact admin.");
        }
        if (finalGate.getStatus() != Status.PASSED && finalGate.getStatus() != Status.NA) {
            throw new IllegalStateException(
                "Cannot mark task COMPLETED — FINAL quality gate is "
                + finalGate.getStatus()
                + ". Site engineer must PASS (or mark NA with justification) the FINAL gate first.");
        }
    }

    private void assertPriorPassed(Long taskId, GateType priorType, String msg) {
        TaskQualityGate prior = gateRepository
                .findByTaskIdAndGateType(taskId, priorType)
                .orElseThrow(() -> new IllegalStateException(
                    priorType + " gate is missing — cannot proceed."));
        if (prior.getStatus() != Status.PASSED && prior.getStatus() != Status.NA) {
            throw new IllegalStateException(msg);
        }
    }
}
