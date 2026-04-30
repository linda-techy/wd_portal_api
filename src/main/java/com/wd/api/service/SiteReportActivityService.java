package com.wd.api.service;

import com.wd.api.dto.SiteReportActivityRequest;
import com.wd.api.model.SiteReport;
import com.wd.api.model.SiteReportActivity;
import com.wd.api.repository.SiteReportActivityRepository;
import com.wd.api.repository.SiteReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for the per-activity manpower breakdown attached to a
 * {@link SiteReport} (V84).
 *
 * <p>Replace-all semantics: callers submit the full list each save —
 * the service deletes the existing rows for that report and inserts
 * the new set. Cleaner than diff-based PUT/DELETE and avoids stale
 * rows lingering when staff drops an activity from the form.
 *
 * <p>Validation lives here (not in the controller) so any future
 * caller — webhook, batch import, mobile sync — gets the same rules.
 */
@Service
public class SiteReportActivityService {

    private final SiteReportRepository siteReportRepository;
    private final SiteReportActivityRepository activityRepository;

    public SiteReportActivityService(SiteReportRepository siteReportRepository,
                                     SiteReportActivityRepository activityRepository) {
        this.siteReportRepository = siteReportRepository;
        this.activityRepository = activityRepository;
    }

    @Transactional(readOnly = true)
    public List<SiteReportActivity> listForReport(Long reportId) {
        return activityRepository.findByReportIdOrderByDisplayOrderAsc(reportId);
    }

    /**
     * Replace the activity set for a report. Each request is validated
     * before any DB write so a bad row doesn't leave the table in a
     * partial state.
     */
    @Transactional
    public List<SiteReportActivity> replaceActivities(Long reportId,
                                                      List<SiteReportActivityRequest> requests) {
        SiteReport report = siteReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Site report not found: " + reportId));

        List<SiteReportActivityRequest> safeRequests = requests != null ? requests : List.of();
        for (SiteReportActivityRequest req : safeRequests) {
            if (req.name() == null || req.name().isBlank()) {
                throw new IllegalArgumentException("Activity name is required");
            }
            if (req.manpower() != null && req.manpower() < 0) {
                throw new IllegalArgumentException("Activity manpower must be >= 0");
            }
        }

        // Wipe existing rows in a single bulk DELETE before re-inserting.
        activityRepository.deleteByReportId(reportId);

        if (safeRequests.isEmpty()) {
            return List.of();
        }

        List<SiteReportActivity> rows = new ArrayList<>(safeRequests.size());
        for (int i = 0; i < safeRequests.size(); i++) {
            SiteReportActivityRequest req = safeRequests.get(i);
            SiteReportActivity row = new SiteReportActivity();
            row.setReport(report);
            row.setName(req.name().trim());
            row.setManpower(req.manpower());
            row.setEquipment(req.equipment());
            row.setNotes(req.notes());
            row.setDisplayOrder(i);
            rows.add(row);
        }
        return activityRepository.saveAll(rows);
    }

    /**
     * Sum of {@code manpower} across all activity rows for the report.
     * Drives the "total men on site today" badge that the customer app
     * still wants alongside the per-activity breakdown. Null
     * {@code manpower} values are ignored (partial data, defensive).
     */
    @Transactional(readOnly = true)
    public int totalManpower(Long reportId) {
        return activityRepository.findByReportIdOrderByDisplayOrderAsc(reportId).stream()
                .map(SiteReportActivity::getManpower)
                .filter(m -> m != null)
                .mapToInt(Integer::intValue)
                .sum();
    }
}
