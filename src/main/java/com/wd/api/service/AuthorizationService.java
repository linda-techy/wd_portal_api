package com.wd.api.service;

import com.wd.api.exception.ResourceNotFoundException;
import com.wd.api.exception.UnauthorizedException;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.model.SiteReport;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.SiteReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized authorization service for checking user permissions.
 * Handles all authorization logic to ensure consistent security enforcement.
 */
@Service
public class AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    private final CustomerProjectRepository projectRepository;
    private final SiteReportRepository siteReportRepository;
    private final PortalUserRepository portalUserRepository;

    public AuthorizationService(CustomerProjectRepository projectRepository,
                                SiteReportRepository siteReportRepository,
                                PortalUserRepository portalUserRepository) {
        this.projectRepository = projectRepository;
        this.siteReportRepository = siteReportRepository;
        this.portalUserRepository = portalUserRepository;
    }

    /**
     * Check if user has access to a specific site report.
     * Throws exception if report doesn't exist or user lacks access.
     *
     * @param userEmail User's email
     * @param reportId Report ID to check
     * @param action Action being performed (for logging)
     * @throws ResourceNotFoundException if report doesn't exist
     * @throws UnauthorizedException if user lacks access
     */
    @Transactional(readOnly = true)
    public void checkSiteReportAccess(String userEmail, Long reportId, String action) {
        logger.debug("Checking site report access for user {} on report {}", userEmail, reportId);

        // Fetch report
        @SuppressWarnings("null") // reportId is validated as non-null by caller
        SiteReport report = siteReportRepository.findById(reportId)
            .orElseThrow(() -> {
                logger.warn("Site report not found: {}", reportId);
                return new ResourceNotFoundException("SiteReport", reportId);
            });

        // Validate project relationship
        if (report.getProject() == null) {
            logger.error("Site report {} has null project reference", reportId);
            throw new ResourceNotFoundException("SiteReport", reportId);
        }

        // Check if user has permission based on role
        PortalUser user = getUserByEmail(userEmail);
        @SuppressWarnings("null")
        Long projectId = report.getProject().getId();
        if (!hasProjectAccess(user, projectId)) {
            logger.warn("User {} unauthorized to {} site report {}", userEmail, action, reportId);
            throw new UnauthorizedException(action, "site report");
        }

        logger.debug("User {} authorized to {} site report {}", userEmail, action, reportId);
    }

    /**
     * Get list of project IDs the user has access to.
     * Results are cached to improve performance (cache expires after 5 minutes).
     *
     * @param userEmail User's email
     * @return List of accessible project IDs
     */
    @Cacheable(value = "userProjects", key = "#userEmail")
    @Transactional(readOnly = true)
    public List<Long> getAccessibleProjectIds(String userEmail) {
        logger.debug("Fetching accessible project IDs for user {} (cache miss)", userEmail);
        
        PortalUser user = getUserByEmail(userEmail);
        List<Long> projectIds;

        // Admin/Super Admin can access all projects
        // PERFORMANCE: Limit to 10000 projects to prevent memory issues with large datasets
        // If more projects exist, consider implementing a more targeted query approach
        if (isAdmin(user)) {
            Pageable pageable = PageRequest.of(0, 10000);
            projectIds = projectRepository.findAll(pageable)
                .stream()
                .map(CustomerProject::getId)
                .collect(Collectors.toList());
        } else {
            // Regular users can only access projects they're assigned to as members
            // PERFORMANCE: Limit to 10000 projects to prevent memory issues with large datasets
            // Note: For better performance, consider a custom repository query that filters by userId in SQL
            @SuppressWarnings("null")
            Long userId = user.getId();
            Pageable pageable = PageRequest.of(0, 10000);
            projectIds = projectRepository.findAll(pageable)
                .stream()
                .filter(project -> project.getProjectMembers().stream()
                    .anyMatch(member -> member.getPortalUser() != null && 
                             member.getPortalUser().getId().equals(userId)))
                .map(CustomerProject::getId)
                .collect(Collectors.toList());
        }

        logger.debug("User {} has access to {} projects", userEmail, projectIds.size());
        return projectIds;
    }

    /**
     * Check if user has access to a specific project.
     * Useful for validating project-level operations.
     *
     * @param userEmail User's email
     * @param projectId Project ID to check
     * @param action Action being performed (for logging)
     * @throws ResourceNotFoundException if project doesn't exist
     * @throws UnauthorizedException if user lacks access
     */
    @Transactional(readOnly = true)
    public void checkProjectAccess(String userEmail, Long projectId, String action) {
        logger.debug("Checking project access for user {} on project {}", userEmail, projectId);

        // Check if project exists
        @SuppressWarnings("null") // projectId is validated as non-null by caller
        boolean projectExists = projectRepository.existsById(projectId);
        if (!projectExists) {
            logger.warn("Project not found: {}", projectId);
            throw new ResourceNotFoundException("Project", projectId);
        }

        // Check if user has access
        PortalUser user = getUserByEmail(userEmail);
        if (!hasProjectAccess(user, projectId)) {
            logger.warn("User {} unauthorized to {} project {}", userEmail, action, projectId);
            throw new UnauthorizedException(action, "project");
        }

        logger.debug("User {} authorized to {} project {}", userEmail, action, projectId);
    }

    /**
     * Check if user has admin privileges.
     *
     * @param userEmail User's email
     * @return true if user is admin or super admin
     */
    @Transactional(readOnly = true)
    public boolean isAdmin(String userEmail) {
        PortalUser user = getUserByEmail(userEmail);
        return isAdmin(user);
    }

    /**
     * Check if user has a specific role.
     *
     * @param userEmail User's email
     * @param roleName Role name to check
     * @return true if user has the specified role
     */
    @Transactional(readOnly = true)
    public boolean hasRole(String userEmail, String roleName) {
        PortalUser user = getUserByEmail(userEmail);
        return user.getRole() != null && 
               user.getRole().getName().equalsIgnoreCase(roleName);
    }

    // ===== PRIVATE HELPER METHODS =====

    private PortalUser getUserByEmail(String email) {
        return portalUserRepository.findByEmail(email)
            .orElseThrow(() -> {
                logger.error("Portal user not found: {}", email);
                return new ResourceNotFoundException("PortalUser", email);
            });
    }

    private boolean isAdmin(PortalUser user) {
        if (user.getRole() == null) {
            return false;
        }
        String roleName = user.getRole().getName();
        return roleName.equalsIgnoreCase("ADMIN") || 
               roleName.equalsIgnoreCase("SUPER_ADMIN");
    }

    private boolean hasProjectAccess(PortalUser user, Long projectId) {
        // Admins have access to all projects
        if (isAdmin(user)) {
            return true;
        }

        // Check if user is assigned to the project as a project member
        @SuppressWarnings("null") // user.getId() and projectId are validated as non-null
        Long userId = user.getId();
        @SuppressWarnings("null") // projectId is validated as non-null by caller
        var hasAccess = projectRepository.findById(projectId)
            .map(project -> project.getProjectMembers().stream()
                .anyMatch(member -> member.getPortalUser() != null && 
                         member.getPortalUser().getId().equals(userId)))
            .orElse(false);
        return hasAccess;
    }
}
