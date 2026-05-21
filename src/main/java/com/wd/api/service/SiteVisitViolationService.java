package com.wd.api.service;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.model.SiteVisitViolation;
import com.wd.api.repository.SiteVisitViolationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Records geofence violations for site-visit check-in / check-out attempts.
 *
 * `record(...)` runs in a NEW transaction (REQUIRES_NEW) so the violation row
 * is committed even though the caller (SiteVisitService) immediately throws
 * an IllegalStateException to block the user — which would otherwise roll back
 * the outer transaction and lose the audit row.
 */
@Service
public class SiteVisitViolationService {

    private static final Logger logger = LoggerFactory.getLogger(SiteVisitViolationService.class);

    private final SiteVisitViolationRepository repository;

    public SiteVisitViolationService(SiteVisitViolationRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(CustomerProject project,
                       PortalUser user,
                       SiteVisitViolation.AttemptType attemptType,
                       double attemptedLat,
                       double attemptedLon,
                       double distanceKm,
                       double allowedRadiusKm,
                       Long visitId,
                       String errorMessage) {
        SiteVisitViolation v = new SiteVisitViolation();
        v.setProject(project);
        v.setUser(user);
        v.setAttemptType(attemptType);
        v.setAttemptedAt(LocalDateTime.now());
        v.setAttemptedLatitude(attemptedLat);
        v.setAttemptedLongitude(attemptedLon);
        if (project != null && project.hasLocation()) {
            v.setProjectLatitude(project.getLatitude());
            v.setProjectLongitude(project.getLongitude());
        }
        v.setDistanceKm(distanceKm);
        v.setAllowedRadiusKm(allowedRadiusKm);
        v.setVisitId(visitId);
        v.setErrorMessage(errorMessage);
        repository.save(v);

        logger.warn("Site-visit geofence violation: user={} project={} attempt={} distance={}km allowed={}km",
                user != null ? user.getId() : null,
                project != null ? project.getId() : null,
                attemptType, distanceKm, allowedRadiusKm);
    }
}
