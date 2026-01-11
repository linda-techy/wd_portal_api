package com.wd.api.service;

import com.wd.api.model.ActivityFeed;
import com.wd.api.model.ActivityType;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.CustomerUser;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.ActivityFeedRepository;
import com.wd.api.repository.ActivityTypeRepository; // Need this
// Need this to find user ? Or AuthService?
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ActivityFeedService {

    @Autowired
    private ActivityFeedRepository activityFeedRepository;

    @Autowired
    private ActivityTypeRepository activityTypeRepository;

    // We might need to resolve the current user here or pass it in.
    // Usually the Service calling this provies the user or we get it from
    // SecurityContext.

    @Transactional
    public void logActivity(String typeName, String title, String description, Long referenceId, String referenceType,
            CustomerUser user) {
        saveActivity(typeName, title, description, referenceId, referenceType, user, null, null);
    }

    @Transactional
    public void logActivity(String typeName, String title, String description, Long referenceId, String referenceType,
            PortalUser user) {
        saveActivity(typeName, title, description, referenceId, referenceType, null, user, null);
    }

    @Transactional
    public void logSystemActivity(String typeName, String title, String description, Long referenceId,
            String referenceType) {
        saveActivity(typeName, title, description, referenceId, referenceType, null, null, null);
    }

    @Transactional
    public void logProjectActivity(String typeName, String title, String description, CustomerProject project,
            PortalUser user) {
        saveActivity(typeName, title, description, project.getId(), "PROJECT", null, user, project);
    }

    private void saveActivity(String typeName, String title, String description, Long referenceId, String referenceType,
            CustomerUser customerUser, PortalUser portalUser, CustomerProject project) {
        ActivityType type = activityTypeRepository.findByName(typeName)
                .orElseThrow(() -> new RuntimeException("Activity Type not found: " + typeName));

        ActivityFeed feed = new ActivityFeed();
        feed.setActivityType(type);
        feed.setTitle(title);
        feed.setDescription(description);
        feed.setReferenceId(referenceId);
        feed.setReferenceType(referenceType);
        feed.setCreatedBy(customerUser);
        if (portalUser != null) {
            feed.setPortalUser(portalUser);
        }
        feed.setCreatedAt(LocalDateTime.now());
        feed.setProject(project);

        // If it's a lead activity, try to link it (we might need LeadRepository for
        // this,
        // but for now let's just ensure if project is set, it passes the constraint)

        activityFeedRepository.save(feed);
    }

    public java.util.List<ActivityFeed> getActivitiesForLead(Long leadId) {
        return activityFeedRepository.findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(leadId, "LEAD");
    }

    /**
     * Get recent activities for a specific project
     * 
     * @param projectId The project ID
     * @return List of activity feeds for the project, ordered by creation date
     *         descending
     */
    @Transactional(readOnly = true)
    public java.util.List<ActivityFeed> getRecentProjectActivities(Long projectId) {
        if (projectId == null) {
            return java.util.List.of();
        }
        return activityFeedRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional
    public void linkLeadActivitiesToProject(Long leadId, CustomerProject project) {
        java.util.List<ActivityFeed> activities = activityFeedRepository
                .findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(leadId, "LEAD");

        for (ActivityFeed activity : activities) {
            activity.setProject(project);
            // Optionally we can keep referenceId/Type as LEAD or update it to PROJECT
            // Keeping it as LEAD but linking to Project is often best for provenance
        }
        activityFeedRepository.saveAll(activities);
    }
}
