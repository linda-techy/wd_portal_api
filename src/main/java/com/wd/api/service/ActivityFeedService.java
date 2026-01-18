package com.wd.api.service;

import com.wd.api.dto.ActivityFeedDTO;
import com.wd.api.model.ActivityFeed;
import com.wd.api.model.ActivityType;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.CustomerUser;
import com.wd.api.model.LeadInteraction;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.ActivityFeedRepository;
import com.wd.api.repository.ActivityTypeRepository;
import com.wd.api.repository.LeadInteractionRepository;
import com.wd.api.repository.PortalUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActivityFeedService {

    @Autowired
    private ActivityFeedRepository activityFeedRepository;

    @Autowired
    private ActivityTypeRepository activityTypeRepository;

    @Autowired
    private LeadInteractionRepository leadInteractionRepository;

    @Autowired
    private PortalUserRepository portalUserRepository;

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

    /**
     * Get activities for a lead and return as DTOs to avoid lazy-loading serialization issues
     * Combines data from both activity_feeds and lead_interactions tables
     * Enterprise-grade pattern: Returns DTOs instead of entities for API responses
     */
    @Transactional(readOnly = true)
    public List<ActivityFeedDTO> getActivitiesForLead(Long leadId) {
        // Fetch from activity_feeds table
        List<ActivityFeed> activityFeeds = activityFeedRepository.findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(leadId, "LEAD");
        List<ActivityFeedDTO> feedDTOs = activityFeeds.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        // Fetch from lead_interactions table
        List<LeadInteraction> interactions = leadInteractionRepository.findByLeadIdOrderByInteractionDateDesc(leadId);
        List<ActivityFeedDTO> interactionDTOs = interactions.stream()
                .map(this::interactionToDTO)
                .collect(Collectors.toList());

        // Combine both lists
        List<ActivityFeedDTO> allActivities = new ArrayList<>();
        allActivities.addAll(feedDTOs);
        allActivities.addAll(interactionDTOs);

        // Sort by date descending (most recent first)
        allActivities.sort(Comparator.comparing(ActivityFeedDTO::getCreatedAt).reversed());

        return allActivities;
    }

    /**
     * Convert ActivityFeed entity to ActivityFeedDTO
     * Safely extracts data from lazy-loaded relationships
     */
    private ActivityFeedDTO toDTO(ActivityFeed feed) {
        String activityTypeName = feed.getActivityType() != null ? feed.getActivityType().getName() : "UNKNOWN";
        String createdByName = null;
        if (feed.getCreatedBy() != null) {
            createdByName = feed.getCreatedBy().getEmail();
        } else if (feed.getPortalUser() != null) {
            createdByName = feed.getPortalUser().getEmail();
        }
        
        return ActivityFeedDTO.builder()
                .id(feed.getId())
                .title(feed.getTitle())
                .description(feed.getDescription())
                .activityType(activityTypeName)
                .createdAt(feed.getCreatedAt())
                .createdByName(createdByName)
                .build();
    }

    /**
     * Convert LeadInteraction entity to ActivityFeedDTO
     * Maps interaction data to activity feed format for unified display
     */
    private ActivityFeedDTO interactionToDTO(LeadInteraction interaction) {
        // Build title from interaction type and subject
        String title = interaction.getSubject() != null && !interaction.getSubject().isEmpty()
                ? interaction.getSubject()
                : interaction.getInteractionType() != null
                        ? interaction.getInteractionType().replace("_", " ")
                        : "Interaction";

        // Build description from notes and outcome
        StringBuilder description = new StringBuilder();
        if (interaction.getNotes() != null && !interaction.getNotes().isEmpty()) {
            description.append(interaction.getNotes());
        }
        if (interaction.getOutcome() != null && !interaction.getOutcome().isEmpty()) {
            if (description.length() > 0) {
                description.append(" | ");
            }
            description.append("Outcome: ").append(interaction.getOutcome());
        }
        if (interaction.getDurationMinutes() != null && interaction.getDurationMinutes() > 0) {
            if (description.length() > 0) {
                description.append(" | ");
            }
            description.append("Duration: ").append(interaction.getDurationMinutes()).append(" min");
        }

        // Get created by name
        String createdByName = null;
        if (interaction.getCreatedById() != null) {
            Optional<PortalUser> userOpt = portalUserRepository.findById(interaction.getCreatedById());
            if (userOpt.isPresent()) {
                createdByName = userOpt.get().getEmail();
            }
        }

        // Use interaction date as the activity date, fallback to createdAt
        LocalDateTime activityDate = interaction.getInteractionDate() != null
                ? interaction.getInteractionDate()
                : interaction.getCreatedAt();

        return ActivityFeedDTO.builder()
                .id(interaction.getId())
                .title(title)
                .description(description.toString())
                .activityType(interaction.getInteractionType() != null
                        ? interaction.getInteractionType()
                        : "INTERACTION")
                .createdAt(activityDate)
                .createdByName(createdByName)
                .build();
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
