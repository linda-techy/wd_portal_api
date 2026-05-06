package com.wd.api.service.scheduling;

import com.wd.api.dto.scheduling.PendingApprovalRowDto;
import com.wd.api.exception.ResourceNotFoundException;
import com.wd.api.model.Task;
import com.wd.api.model.enums.ReportType;
import com.wd.api.repository.SiteReportRepository;
import com.wd.api.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * S3 PR2 — Completion-gate FSM.
 *
 * <p>Linear state machine:
 * <pre>
 *   IN_PROGRESS ──markComplete()──▶ COMPLETED          (requires_pm_approval=false)
 *   IN_PROGRESS ──markComplete()──▶ PENDING_PM_APPROVAL (requires_pm_approval=true)
 *   PENDING_PM_APPROVAL ──approveCompletion()──▶ COMPLETED
 *   PENDING_PM_APPROVAL ──rejectCompletion(reason)──▶ IN_PROGRESS
 * </pre>
 *
 * <p>Every transition calls {@link CpmService#recompute(Long)} after the save
 * so downstream task EF/LF dates and the project's expected handover stay
 * authoritative.
 *
 * <p>Site-engineer markComplete is gated on at least one geotagged
 * SiteReport with {@code reportType=COMPLETION} for the task; PM
 * approve/reject are gated on the {@code TASK_COMPLETION_APPROVE} permission
 * (controller-level @PreAuthorize).
 */
@Service
public class TaskCompletionService {

    private final TaskRepository taskRepo;
    private final SiteReportRepository siteReportRepo;
    private final ProjectScheduleConfigService configService;
    private final CpmService cpmService;

    public TaskCompletionService(TaskRepository taskRepo,
                                 SiteReportRepository siteReportRepo,
                                 ProjectScheduleConfigService configService,
                                 CpmService cpmService) {
        this.taskRepo = taskRepo;
        this.siteReportRepo = siteReportRepo;
        this.configService = configService;
        this.cpmService = cpmService;
    }

    @Transactional
    public Task markComplete(Long taskId, Long userId) {
        Task t = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + taskId + " not found"));

        if (t.getStatus() != Task.TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Task " + taskId + " is not in progress (status=" + t.getStatus() + ")");
        }
        if (!hasGeotaggedCompletionPhoto(taskId)) {
            throw new IllegalStateException(
                    "Geotagged completion photo required before marking task " + taskId + " complete");
        }

        Long projectId = t.getProject().getId();
        boolean requiresApproval = Boolean.TRUE.equals(
                configService.get(projectId).requiresPmApproval());

        t.setStatus(requiresApproval
                ? Task.TaskStatus.PENDING_PM_APPROVAL
                : Task.TaskStatus.COMPLETED);
        t.setActualEndDate(LocalDate.now());

        Task saved = taskRepo.save(t);
        cpmService.recompute(projectId);
        return saved;
    }

    @Transactional
    public Task approveCompletion(Long taskId, Long userId) {
        Task t = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + taskId + " not found"));

        if (t.getStatus() != Task.TaskStatus.PENDING_PM_APPROVAL) {
            throw new IllegalStateException(
                    "Task " + taskId + " is not pending approval (status=" + t.getStatus() + ")");
        }

        t.setStatus(Task.TaskStatus.COMPLETED);
        t.setRejectionReason(null);
        Task saved = taskRepo.save(t);
        cpmService.recompute(t.getProject().getId());
        return saved;
    }

    @Transactional
    public Task rejectCompletion(Long taskId, Long userId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        Task t = taskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + taskId + " not found"));

        if (t.getStatus() != Task.TaskStatus.PENDING_PM_APPROVAL) {
            throw new IllegalStateException(
                    "Task " + taskId + " is not pending approval (status=" + t.getStatus() + ")");
        }

        t.setStatus(Task.TaskStatus.IN_PROGRESS);
        t.setActualEndDate(null);
        t.setRejectionReason(reason);
        Task saved = taskRepo.save(t);
        cpmService.recompute(t.getProject().getId());
        return saved;
    }

    /**
     * S3 PR2 — PM Approval Inbox query. Pulls tasks in PENDING_PM_APPROVAL
     * for projects the caller is a member of and joins each row with the
     * most recent geotagged COMPLETION SiteReport's first photo URL.
     */
    @Transactional(readOnly = true)
    public List<PendingApprovalRowDto> findPendingApprovalsForUser(Long userId) {
        List<Task> pending = taskRepo.findPendingPmApprovalForUser(userId);
        List<PendingApprovalRowDto> out = new ArrayList<>(pending.size());
        for (Task t : pending) {
            String photoUrl = siteReportRepo
                    .findFirstByTaskIdAndReportTypeOrderByReportDateDesc(t.getId(), ReportType.COMPLETION)
                    .flatMap(sr -> sr.getPhotos().stream().findFirst())
                    .map(p -> p.getPhotoUrl())
                    .orElse(null);
            out.add(new PendingApprovalRowDto(
                    t.getId(), t.getTitle(),
                    t.getProject().getId(), t.getProject().getName(),
                    t.getActualEndDate(), photoUrl));
        }
        return out;
    }

    private boolean hasGeotaggedCompletionPhoto(Long taskId) {
        return siteReportRepo.existsByTaskIdAndReportTypeAndLatitudeIsNotNullAndLongitudeIsNotNull(
                taskId, ReportType.COMPLETION);
    }
}
