package com.wd.api.service.scheduling;

import com.wd.api.model.scheduling.ProjectBaseline;
import com.wd.api.model.scheduling.ProjectScheduleConfig;
import com.wd.api.repository.ProjectBaselineRepository;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.WebhookPublisherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

/**
 * After a CPM recompute, decides whether the customer's expected handover
 * has shifted enough to warrant an FCM push.
 *
 * <p>Compares the project's current handover (max {@code task.ef_date}) against
 * either the {@link ProjectScheduleConfig#getLastAlertedHandoverDate()} (if
 * present — gives a per-project cooldown so small CPM jitter doesn't spam the
 * customer) or, on first-ever alert, the approved
 * {@link ProjectBaseline#getProjectFinishDate()}.
 *
 * <p>If neither is available (no prior alert AND no baseline → first CPM run
 * after project creation, before baseline approval), no alert fires.
 *
 * <p>Threshold: {@value #THRESHOLD_WORKING_DAYS} working days. Both directions
 * count — handover moving earlier is also news to the customer.
 */
@Service
public class HandoverShiftDetector {

    private static final Logger log = LoggerFactory.getLogger(HandoverShiftDetector.class);
    private static final int THRESHOLD_WORKING_DAYS = 3;

    private final TaskRepository taskRepo;
    private final ProjectScheduleConfigRepository configRepo;
    private final ProjectBaselineRepository baselineRepo;
    private final HolidayService holidayService;
    private final WebhookPublisherService webhookPublisher;

    public HandoverShiftDetector(TaskRepository taskRepo,
                                 ProjectScheduleConfigRepository configRepo,
                                 ProjectBaselineRepository baselineRepo,
                                 HolidayService holidayService,
                                 WebhookPublisherService webhookPublisher) {
        this.taskRepo = taskRepo;
        this.configRepo = configRepo;
        this.baselineRepo = baselineRepo;
        this.holidayService = holidayService;
        this.webhookPublisher = webhookPublisher;
    }

    @Transactional
    public void checkAndAlert(Long projectId) {
        Optional<LocalDate> maxEf = taskRepo.findMaxEfDateByProjectId(projectId);
        if (maxEf.isEmpty()) {
            log.debug("HandoverShiftDetector: project={} has no EF dates yet — skipping", projectId);
            return;
        }
        LocalDate currentHandover = maxEf.get();

        ProjectScheduleConfig cfg = configRepo.findByProjectId(projectId).orElse(null);
        if (cfg == null) {
            log.debug("HandoverShiftDetector: project={} has no schedule config — skipping", projectId);
            return;
        }

        LocalDate compareAgainst = cfg.getLastAlertedHandoverDate();
        if (compareAgainst == null) {
            // First-ever alert candidate; baseline is the only valid reference.
            compareAgainst = baselineRepo.findByProjectId(projectId)
                    .map(ProjectBaseline::getProjectFinishDate)
                    .orElse(null);
        }
        if (compareAgainst == null) {
            // No baseline AND no prior alert — first CPM after project creation.
            log.debug("HandoverShiftDetector: project={} no prior alert, no baseline — skipping", projectId);
            return;
        }

        boolean sundayWorking = Boolean.TRUE.equals(cfg.getSundayWorking());
        // Holiday window must span both endpoints regardless of direction.
        LocalDate windowStart = compareAgainst.isBefore(currentHandover) ? compareAgainst : currentHandover;
        LocalDate windowEnd = compareAgainst.isBefore(currentHandover) ? currentHandover : compareAgainst;
        Set<LocalDate> holidays = holidayService.holidaysFor(projectId, windowStart, windowEnd);

        // Signed shift: positive if current is later, negative if earlier.
        // WorkingDayCalculator.workingDaysBetween throws when end < start, so
        // branch and negate (mirrors CpmService's float computation pattern).
        int shift;
        if (currentHandover.isBefore(compareAgainst)) {
            shift = -WorkingDayCalculator.workingDaysBetween(
                    currentHandover, compareAgainst, holidays, sundayWorking);
        } else {
            shift = WorkingDayCalculator.workingDaysBetween(
                    compareAgainst, currentHandover, holidays, sundayWorking);
        }

        if (Math.abs(shift) <= THRESHOLD_WORKING_DAYS) {
            log.debug("HandoverShiftDetector: project={} shift={} working days — within threshold",
                    projectId, shift);
            return;
        }

        log.info("HandoverShiftDetector: project={} handover shifted from {} to {} ({} working days)",
                projectId, compareAgainst, currentHandover, shift);
        webhookPublisher.publishHandoverShifted(projectId, compareAgainst, currentHandover, shift);
        cfg.setLastAlertedHandoverDate(currentHandover);
        configRepo.save(cfg);
    }
}
