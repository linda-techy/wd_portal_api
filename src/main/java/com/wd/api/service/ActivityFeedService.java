package com.wd.api.service;

import com.wd.api.model.ActivityFeed;
import com.wd.api.model.ActivityType;
import com.wd.api.model.CustomerUser;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.ActivityFeedRepository;
import com.wd.api.repository.ActivityTypeRepository; // Need this
import com.wd.api.repository.CustomerUserRepository; // Need this to find user ? Or AuthService?
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

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
        saveActivity(typeName, title, description, referenceId, referenceType, user, null);
    }

    @Transactional
    public void logActivity(String typeName, String title, String description, Long referenceId, String referenceType,
            PortalUser user) {
        saveActivity(typeName, title, description, referenceId, referenceType, null, user);
    }

    @Transactional
    public void logSystemActivity(String typeName, String title, String description, Long referenceId,
            String referenceType) {
        saveActivity(typeName, title, description, referenceId, referenceType, null, null);
    }

    private void saveActivity(String typeName, String title, String description, Long referenceId, String referenceType,
            CustomerUser customerUser, PortalUser portalUser) {
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

        feed.setProject(null);

        activityFeedRepository.save(feed);
    }

    public java.util.List<ActivityFeed> getActivitiesForLead(Long leadId) {
        return activityFeedRepository.findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(leadId, "LEAD");
    }
}
