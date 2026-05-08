package com.wd.api.service.changerequest;

import com.wd.api.dto.changerequest.ChangeRequestTaskCreateRequest;
import com.wd.api.dto.changerequest.ChangeRequestTaskPredecessorRequest;
import com.wd.api.dto.changerequest.ChangeRequestTaskUpdateRequest;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.changerequest.ChangeRequestTask;
import com.wd.api.model.changerequest.ChangeRequestTaskPredecessor;
import com.wd.api.model.enums.FloorLoop;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.repository.ProjectVariationRepository;
import com.wd.api.repository.changerequest.ChangeRequestTaskPredecessorRepository;
import com.wd.api.repository.changerequest.ChangeRequestTaskRepository;
import com.wd.api.service.scheduling.TaskGraphValidator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Author-time CRUD over a CR's proposed scope. Edits are only allowed while
 * the parent CR is in DRAFT or SUBMITTED status (i.e., before QS has costed
 * it). Once COSTED or beyond, the proposed scope is "frozen" — further edits
 * would invalidate the cost analysis and require a fresh CR.
 */
@Service
public class ChangeRequestTaskService {

    private static final Set<VariationStatus> EDITABLE =
            EnumSet.of(VariationStatus.DRAFT, VariationStatus.SUBMITTED);

    private final ChangeRequestTaskRepository taskRepo;
    private final ChangeRequestTaskPredecessorRepository predRepo;
    private final ProjectVariationRepository crRepo;

    public ChangeRequestTaskService(ChangeRequestTaskRepository taskRepo,
                                    ChangeRequestTaskPredecessorRepository predRepo,
                                    ProjectVariationRepository crRepo) {
        this.taskRepo = taskRepo;
        this.predRepo = predRepo;
        this.crRepo = crRepo;
    }

    @Transactional
    public ChangeRequestTask addTask(Long crId, ChangeRequestTaskCreateRequest req, Long actorUserId) {
        ProjectVariation cr = loadEditable(crId);
        long count = taskRepo.countByChangeRequestId(crId);
        ChangeRequestTask t = new ChangeRequestTask();
        t.setChangeRequest(cr);
        t.setSequence((int) (count + 1));
        t.setName(req.getName());
        t.setRoleHint(req.getRoleHint());
        t.setDurationDays(req.getDurationDays() != null ? req.getDurationDays() : 1);
        t.setWeightFactor(req.getWeightFactor());
        t.setMonsoonSensitive(req.getMonsoonSensitive());
        t.setIsPaymentMilestone(req.getIsPaymentMilestone());
        t.setFloorLoop(req.getFloorLoop() != null ? req.getFloorLoop() : FloorLoop.NONE);
        t.setOptionalCost(req.getOptionalCost());
        return taskRepo.save(t);
    }

    @Transactional
    public ChangeRequestTask updateTask(Long crId, Long crTaskId,
                                        ChangeRequestTaskUpdateRequest req, Long actorUserId) {
        loadEditable(crId);
        ChangeRequestTask t = taskRepo.findById(crTaskId)
                .orElseThrow(() -> new EntityNotFoundException("CR task " + crTaskId));
        if (!t.getChangeRequest().getId().equals(crId)) {
            throw new IllegalArgumentException(
                    "CR task " + crTaskId + " does not belong to CR " + crId);
        }
        if (req.getName() != null)             t.setName(req.getName());
        if (req.getRoleHint() != null)         t.setRoleHint(req.getRoleHint());
        if (req.getDurationDays() != null)     t.setDurationDays(req.getDurationDays());
        if (req.getWeightFactor() != null)     t.setWeightFactor(req.getWeightFactor());
        if (req.getMonsoonSensitive() != null) t.setMonsoonSensitive(req.getMonsoonSensitive());
        if (req.getIsPaymentMilestone() != null) t.setIsPaymentMilestone(req.getIsPaymentMilestone());
        if (req.getFloorLoop() != null)        t.setFloorLoop(req.getFloorLoop());
        if (req.getOptionalCost() != null)     t.setOptionalCost(req.getOptionalCost());
        return taskRepo.save(t);
    }

    @Transactional
    public void removeTask(Long crId, Long crTaskId, Long actorUserId) {
        loadEditable(crId);
        ChangeRequestTask t = taskRepo.findById(crTaskId)
                .orElseThrow(() -> new EntityNotFoundException("CR task " + crTaskId));
        if (!t.getChangeRequest().getId().equals(crId)) {
            throw new IllegalArgumentException(
                    "CR task " + crTaskId + " does not belong to CR " + crId);
        }
        taskRepo.delete(t);
    }

    @Transactional
    public ChangeRequestTaskPredecessor addPredecessor(Long crId,
                                                       ChangeRequestTaskPredecessorRequest req,
                                                       Long actorUserId) {
        loadEditable(crId);
        Long succ = req.getSuccessorCrTaskId();
        Long pred = req.getPredecessorCrTaskId();
        if (!taskRepo.existsByIdAndChangeRequestId(succ, crId)
                || !taskRepo.existsByIdAndChangeRequestId(pred, crId)) {
            throw new IllegalArgumentException(
                    "Both endpoints must belong to the same CR " + crId);
        }
        // In-CR cycle check: walk the existing predecessor graph from `pred`
        // and assert `succ` is unreachable. Self-loop falls out of the
        // validator's first guard.
        TaskGraphValidator.assertNoCycle(succ, pred, this::predecessorsOf);

        ChangeRequestTaskPredecessor edge = new ChangeRequestTaskPredecessor(succ, pred, req.getLagDays());
        return predRepo.save(edge);
    }

    @Transactional(readOnly = true)
    public List<ChangeRequestTask> listTasks(Long crId) {
        return taskRepo.findByChangeRequestIdOrderBySequenceAsc(crId);
    }

    @Transactional(readOnly = true)
    public List<ChangeRequestTaskPredecessor> listPredecessors(Long crId) {
        return predRepo.findByChangeRequestId(crId);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private ProjectVariation loadEditable(Long crId) {
        ProjectVariation cr = crRepo.findById(crId)
                .orElseThrow(() -> new EntityNotFoundException("CR " + crId));
        if (!EDITABLE.contains(cr.getStatus())) {
            throw new IllegalStateException(
                    "CR " + crId + " is " + cr.getStatus() + " — proposed scope is frozen "
                  + "(only DRAFT or SUBMITTED CRs may be edited)");
        }
        return cr;
    }

    private List<Long> predecessorsOf(Long crTaskId) {
        return predRepo.findBySuccessorCrTaskId(crTaskId).stream()
                .map(ChangeRequestTaskPredecessor::getPredecessorCrTaskId)
                .toList();
    }
}
