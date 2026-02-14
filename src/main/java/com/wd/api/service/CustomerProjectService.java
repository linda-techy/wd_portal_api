package com.wd.api.service;

import com.wd.api.dto.CustomerProjectCreateRequest;
import com.wd.api.dto.CustomerProjectResponse;
import com.wd.api.dto.CustomerProjectUpdateRequest;
import com.wd.api.dto.ProjectSearchFilter;
import com.wd.api.model.CustomerProject;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.CustomerUserRepository;
import com.wd.api.repository.ProjectMemberRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.LeadRepository;
import com.wd.api.repository.ActivityFeedRepository;
import com.wd.api.model.ActivityFeed;
import com.wd.api.model.ProjectMember;
import com.wd.api.util.SpecificationBuilder;

import com.wd.api.model.PortalUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/**
 * Service layer for Customer Project business logic
 * Extracted from CustomerProjectController to follow Clean Architecture
 * principles
 * Focuses on core CRUD operations with proper transaction management
 */
@Service
@Transactional
public class CustomerProjectService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerProjectService.class);

    @Autowired
    private CustomerProjectRepository customerProjectRepository;

    @Autowired
    private CustomerUserRepository customerUserRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private ActivityFeedRepository activityFeedRepository;

    /**
     * NEW: Standardized search method using ProjectSearchFilter
     * Enterprise-grade implementation with comprehensive filtering
     * Returns DTOs to avoid Hibernate lazy-loading proxy serialization issues
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<CustomerProjectResponse> search(ProjectSearchFilter filter) {
        try {
            Specification<CustomerProject> spec = buildSearchSpecification(filter);
            Pageable pageable = filter.toPageable();
            Page<CustomerProject> projectPage = customerProjectRepository.findAll(spec, pageable);

            // Map entities to DTOs to avoid Hibernate proxy serialization issues
            return projectPage.map(CustomerProjectResponse::new);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid filter parameter in search: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Invalid search filter: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error searching customer projects", e);
            throw new RuntimeException("Error searching projects: " + e.getMessage(), e);
        }
    }

    /**
     * Build JPA Specification from ProjectSearchFilter
     */
    private Specification<CustomerProject> buildSearchSpecification(ProjectSearchFilter filter) {
        SpecificationBuilder<CustomerProject> builder = new SpecificationBuilder<>();

        // Search across multiple fields
        Specification<CustomerProject> searchSpec = builder.buildSearch(
                filter.getSearchQuery(),
                "name", "location", "code", "state", "city", "district");

        // Apply filters with safe enum conversion
        Specification<CustomerProject> phaseSpec = null;
        if (filter.getPhase() != null && !filter.getPhase().trim().isEmpty()) {
            try {
                final String finalPhaseValue = filter.getPhase().trim().toUpperCase().replace(' ', '_');
                phaseSpec = (root, query, cb) -> cb.equal(root.get("projectPhase"),
                        com.wd.api.model.enums.ProjectPhase.valueOf(finalPhaseValue));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid project phase value: {}", filter.getPhase());
                // Skip invalid phase filter instead of failing
            }
        }

        Specification<CustomerProject> typeSpec = builder.buildEquals("projectType", filter.getType());
        Specification<CustomerProject> contractTypeSpec = null;
        if (filter.getContractType() != null && !filter.getContractType().trim().isEmpty()) {
            try {
                final String contractTypeValue = filter.getContractType().toUpperCase();
                contractTypeSpec = (root, query, cb) -> cb.equal(root.get("contractType"),
                        com.wd.api.model.enums.ContractType.valueOf(contractTypeValue));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid contract type value: {}", filter.getContractType());
                // Skip invalid contract type filter instead of failing
            }
        }

        Specification<CustomerProject> statusSpec = builder.buildEquals("status", filter.getStatus());
        Specification<CustomerProject> locationSpec = builder.buildLike("location", filter.getLocation());
        Specification<CustomerProject> citySpec = builder.buildLike("city", filter.getCity());
        Specification<CustomerProject> stateSpec = builder.buildEquals("state", filter.getState());

        // Manager filter
        Specification<CustomerProject> managerSpec = null;
        if (filter.getManagerId() != null) {
            managerSpec = (root, query, cb) -> cb.equal(root.get("projectManager").get("id"), filter.getManagerId());
        }

        // Customer filter
        Specification<CustomerProject> customerSpec = null;
        if (filter.getCustomerId() != null) {
            customerSpec = (root, query, cb) -> cb.equal(root.get("customer").get("id"), filter.getCustomerId());
        }

        // Budget range
        Specification<CustomerProject> budgetSpec = builder.buildNumericRange(
                "estimatedBudget",
                filter.getMinBudget(),
                filter.getMaxBudget());

        // Progress range
        Specification<CustomerProject> progressSpec = builder.buildNumericRange(
                "overallProgress",
                filter.getMinProgress(),
                filter.getMaxProgress());

        // Date range (on start date)
        Specification<CustomerProject> dateRangeSpec = builder.buildDateRange(
                "startDate",
                filter.getStartDate(),
                filter.getEndDate());

        // Combine all specifications
        return builder.and(
                searchSpec,
                phaseSpec,
                typeSpec,
                contractTypeSpec,
                statusSpec,
                managerSpec,
                customerSpec,
                locationSpec,
                citySpec,
                stateSpec,
                budgetSpec,
                progressSpec,
                dateRangeSpec);
    }

    /**
     * DEPRECATED: Old method - kept for backward compatibility
     * Use search(ProjectSearchFilter) instead
     */
    @Deprecated
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<CustomerProjectResponse> getAllProjects(String search, Pageable pageable) {
        Page<CustomerProject> projectPage;
        Pageable resolvedPageable = pageable != null ? pageable : PageRequest.of(0, 10);

        if (search != null && !search.trim().isEmpty()) {
            final String searchPattern = search.trim();
            projectPage = customerProjectRepository
                    .findByNameContainingIgnoreCaseOrLocationContainingIgnoreCaseOrStateContainingIgnoreCaseOrProjectPhaseContainingIgnoreCase(
                            searchPattern, searchPattern, searchPattern, searchPattern, resolvedPageable);
        } else {
            projectPage = customerProjectRepository.findAll(resolvedPageable);
        }

        return projectPage.map(CustomerProjectResponse::new);
    }

    /**
     * Get project by ID
     */
    @Transactional(readOnly = true)
    public Optional<CustomerProject> getProjectById(Long id) {
        if (id == null)
            return Optional.empty();
        return customerProjectRepository.findById(id);
    }

    /**
     * Get projects by lead ID
     */
    @Transactional(readOnly = true)
    public List<CustomerProject> getProjectsByLeadId(Long leadId) {
        if (leadId == null)
            return List.of();
        return customerProjectRepository.findByLeadId(leadId);
    }

    /**
     * Create new customer project
     */
    @SuppressWarnings("null")
    public CustomerProject createProject(CustomerProjectCreateRequest request, String createdBy) {
        // Validate required fields
        validateProjectRequest(request);

        // Create new project
        CustomerProject project = new CustomerProject();
        project.setName(request.getName().trim());
        project.setLocation(request.getLocation().trim());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());

        // Set defaults with enum conversion (normalize spaces to underscores for ON_HOLD)
        if (request.getProjectPhase() != null && !request.getProjectPhase().trim().isEmpty()) {
            try {
                String phaseValue = request.getProjectPhase().trim().toUpperCase().replace(' ', '_');
                project.setProjectPhase(com.wd.api.model.enums.ProjectPhase.valueOf(phaseValue));
            } catch (IllegalArgumentException e) {
                project.setProjectPhase(com.wd.api.model.enums.ProjectPhase.PLANNING);
            }
        } else {
            project.setProjectPhase(com.wd.api.model.enums.ProjectPhase.PLANNING);
        }
        project.setProjectType(request.getProjectType() != null && !request.getProjectType().trim().isEmpty()
                ? request.getProjectType().trim()
                : "turnkey_project");
        project.setState(request.getState() != null && !request.getState().trim().isEmpty()
                ? request.getState().trim()
                : "Kerala");
        project.setDistrict(request.getDistrict() != null && !request.getDistrict().trim().isEmpty()
                ? request.getDistrict().trim()
                : null);

        project.setSqfeet(request.getSqfeet());
        project.setLeadId(request.getLeadId());

        // Initial dummy code, will be updated after save
        project.setCode("PENDING");

        // Set customer if provided
        if (request.getCustomerId() != null) {
            Long customerId = request.getCustomerId();
            customerUserRepository.findById(customerId)
                    .ifPresent(project::setCustomer);
        }

        // Handle contract type
        if (request.getContractType() != null && !request.getContractType().trim().isEmpty()) {
            try {
                project.setContractType(
                        com.wd.api.model.enums.ContractType.valueOf(request.getContractType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid contract type provided: {}, defaulting to TURNKEY", request.getContractType());
                project.setContractType(com.wd.api.model.enums.ContractType.TURNKEY);
            }
        } else {
            project.setContractType(com.wd.api.model.enums.ContractType.TURNKEY);
        }

        // Save Project to get ID
        CustomerProject savedProject = customerProjectRepository.save(project);

        // Update Lead status if leadId is present
        if (savedProject.getLeadId() != null) {
            Long leadId = savedProject.getLeadId();
            leadRepository.findById(leadId).ifPresent(lead -> {
                lead.setLeadStatus("WON");
                lead.setConvertedAt(java.time.LocalDateTime.now());
                // Try to find user ID by email/username 'createdBy'
                portalUserRepository.findByEmail(createdBy).ifPresent(user -> {
                    lead.setConvertedById(user.getId());
                });
                leadRepository.save(lead);
            });
        }

        // Generate Enterprise Project Code: PRJ-{YEAR}-{ID}
        String projectCode = "PRJ-" + java.time.LocalDate.now().getYear() + "-"
                + String.format("%04d", savedProject.getId());
        savedProject.setCode(projectCode);

        // Handle Project Manager
        if (request.getProjectManagerId() != null) {
            savedProject.setProjectManagerId(request.getProjectManagerId());
            syncProjectManagerMember(savedProject, request.getProjectManagerId());
        }

        return customerProjectRepository.save(savedProject);
    }

    /**
     * Update existing customer project
     */
    public CustomerProject updateProject(Long id, CustomerProjectUpdateRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("Project ID is required");
        }
        // Find existing project
        CustomerProject project = customerProjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project with ID " + id + " not found"));

        // Validate request
        validateProjectUpdateRequest(request);

        // Validate dates
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("End date must be after start date");
            }
        }

        // Validate code uniqueness if code is being changed
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            String newCode = request.getCode().trim();
            String existingCode = project.getCode();

            boolean codeChanged = (existingCode == null || !newCode.equals(existingCode.trim()));

            if (codeChanged) {
                Optional<CustomerProject> existingProject = customerProjectRepository.findByCode(newCode);
                if (existingProject.isPresent() && !existingProject.get().getId().equals(id)) {
                    throw new IllegalArgumentException(
                            "Project code '" + newCode + "' already exists for project ID "
                                    + existingProject.get().getId());
                }
            }
        }

        // Update fields
        project.setName(request.getName().trim());
        project.setLocation(request.getLocation().trim());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        // Update project phase with enum conversion (normalize spaces to underscores for ON_HOLD)
        if (request.getProjectPhase() != null && !request.getProjectPhase().trim().isEmpty()) {
            try {
                String phaseValue = request.getProjectPhase().trim().toUpperCase().replace(' ', '_');
                project.setProjectPhase(com.wd.api.model.enums.ProjectPhase.valueOf(phaseValue));
            } catch (IllegalArgumentException e) {
                // Keep existing value if invalid enum provided
            }
        }
        project.setProjectType(request.getProjectType() != null && !request.getProjectType().trim().isEmpty()
                ? request.getProjectType().trim()
                : null);
        project.setState(request.getState() != null && !request.getState().trim().isEmpty()
                ? request.getState().trim()
                : null);
        project.setDistrict(request.getDistrict() != null && !request.getDistrict().trim().isEmpty()
                ? request.getDistrict().trim()
                : null);
        project.setSqfeet(request.getSqfeet());
        project.setLeadId(request.getLeadId());

        // Update code if provided (manual override)
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            project.setCode(request.getCode().trim());
        }

        // Handle Project Manager Update
        if (request.getProjectManagerId() != null) {
            project.setProjectManagerId(request.getProjectManagerId());
            syncProjectManagerMember(project, request.getProjectManagerId());
        }

        return customerProjectRepository.save(project);
    }

    /**
     * Delete customer project with safe hard delete check
     * Enterprise-grade deletion: Cascades deletion of activity feeds (audit logs)
     * while preserving business-critical data (tasks, invoices, payments, etc.)
     * that require explicit deletion for compliance and audit trail purposes
     */
    @SuppressWarnings("null")
    public void deleteProject(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Project ID is required");
        }
        CustomerProject project = customerProjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project with ID " + id + " not found"));

        try {
            // Step 1: Delete activity feeds (audit logs) that reference this project
            // Activity feeds are non-critical logs that can be safely cascaded
            List<ActivityFeed> activityFeeds = activityFeedRepository.findByProjectIdOrderByCreatedAtDesc(id);
            if (!activityFeeds.isEmpty()) {
                logger.info("Deleting {} activity feed(s) for project ID: {}", activityFeeds.size(), id);
                activityFeedRepository.deleteAll(activityFeeds);
                activityFeedRepository.flush(); // Ensure deletion is committed before project deletion
                logger.debug("Activity feeds deleted successfully for project ID: {}", id);
            }

            // Step 2: Attempt to delete the project
            // If Foreign Keys exist (Tasks, Invoices, Payments, etc.), this will fail with
            // DataIntegrityViolationException - these entities require explicit deletion
            // for business compliance and audit trail purposes
            customerProjectRepository.delete(project);
            logger.info("Customer project deleted successfully - ID: {}", id);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Extract constraint information from the exception
            String constraintMessage = extractConstraintInfo(e);
            logger.error("Cannot delete project ID {} due to existing related data: {}", id, constraintMessage, e);

            // Provide detailed error message for business-critical entities
            throw new IllegalStateException(
                    "Cannot delete project because it contains related business data. " +
                            "Please delete the following related data first: Tasks, Invoices, Payments, " +
                            "Purchase Orders, Subcontract Work Orders, Material Indents, " +
                            "Project Warranty records, Measurement Books, or other business-critical entities. " +
                            "Error details: " + constraintMessage);
        } catch (Exception e) {
            logger.error("Unexpected error deleting project ID: {}", id, e);
            throw new RuntimeException("Failed to delete project: " + e.getMessage(), e);
        }
    }

    /**
     * Extract constraint information from DataIntegrityViolationException
     * for better error reporting
     */
    private String extractConstraintInfo(org.springframework.dao.DataIntegrityViolationException e) {
        String message = e.getMessage();
        if (message != null && message.contains("violates foreign key constraint")) {
            // Extract constraint name and table name from the message
            // Example: "violates foreign key constraint "fk8vxso9srli6g5ys0gw6md5vwu" on
            // table "activity_feeds""
            try {
                int constraintStart = message.indexOf("constraint \"");
                int constraintEnd = message.indexOf("\"", constraintStart + 12);
                int tableStart = message.indexOf("table \"");
                int tableEnd = message.indexOf("\"", tableStart + 7);

                if (constraintStart > 0 && constraintEnd > constraintStart &&
                        tableStart > 0 && tableEnd > tableStart) {
                    String tableName = message.substring(tableStart + 7, tableEnd);
                    return "Foreign key constraint violation on table: " + tableName;
                }
            } catch (Exception ex) {
                logger.debug("Could not parse constraint info from exception message", ex);
            }
        }
        return message != null ? message : "Unknown constraint violation";
    }

    // ==================== Private Helper Methods ====================

    private void validateProjectRequest(CustomerProjectCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Project name is required");
        }
        if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Location is required");
        }
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("End date must be after start date");
            }
        }
    }

    private void validateProjectUpdateRequest(CustomerProjectUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Project name is required");
        }
        if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Location is required");
        }
    }

    private void syncProjectManagerMember(CustomerProject project, Long pmId) {
        if (pmId == null)
            return;
        PortalUser pmUser = portalUserRepository.findById(pmId).orElse(null);
        if (pmUser == null)
            return;

        // Check if PM member exists
        java.util.Set<ProjectMember> members = project.getProjectMembers();
        ProjectMember pmMember = null;

        if (members != null) {
            pmMember = members.stream()
                    .filter(m -> "PROJECT_MANAGER".equals(m.getRoleInProject()))
                    .findFirst()
                    .orElse(null);
        }

        if (pmMember != null) {
            // Update existing
            pmMember.setPortalUser(pmUser);
        } else {
            // Create new
            pmMember = new ProjectMember();
            pmMember.setProject(project);
            pmMember.setPortalUser(pmUser);
            pmMember.setRoleInProject("PROJECT_MANAGER");

            // Note: Since we are using CascadeType.ALL on project.getProjectMembers,
            // adding to the set should be enough if we save the project.
            // But here we are explicit to be safe.
            // However, the project might not have the collection initialized if fetched
            // lazily?
            // Safer to use repository directly if we can, or just save member.
            projectMemberRepository.save(pmMember);
        }
    }

    /**
     * Get project statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProjectStats() {
        Map<String, Object> stats = new HashMap<>();

        // Total projects count
        long totalProjects = customerProjectRepository.count();
        stats.put("total_projects", totalProjects);

        // Projects by phase
        List<Object[]> projectsByPhase = customerProjectRepository.getProjectCountByPhase();
        Map<String, Long> phaseMap = new HashMap<>();
        for (Object[] row : projectsByPhase) {
            if (row[0] != null) {
                phaseMap.put(row[0].toString(), (Long) row[1]);
            }
        }
        stats.put("projects_by_phase", phaseMap);

        // Projects by status
        List<Object[]> projectsByStatus = customerProjectRepository.getProjectCountByStatus();
        Map<String, Long> statusMap = new HashMap<>();
        long activeCount = 0;
        long completedCount = 0;
        for (Object[] row : projectsByStatus) {
            if (row[0] != null) {
                String status = row[0].toString();
                Long count = (Long) row[1];
                statusMap.put(status, count);
                if ("COMPLETED".equals(status)) {
                    completedCount = count;
                } else if ("IN_PROGRESS".equals(status) || "ON_TRACK".equals(status)) {
                    activeCount += count;
                }
            }
        }
        stats.put("projects_by_status", statusMap);
        stats.put("active_projects", activeCount);
        stats.put("completed_projects", completedCount);

        // Calculate completion rate
        double completionRate = totalProjects > 0 ? (completedCount * 100.0 / totalProjects) : 0.0;
        stats.put("completion_rate", Math.round(completionRate * 10.0) / 10.0);

        // Get overdue projects
        List<CustomerProject> overdueProjects = customerProjectRepository.findOverdueProjects();
        stats.put("delayed_count", (long) overdueProjects.size());
        stats.put("on_track_count", activeCount - overdueProjects.size());

        return stats;
    }
}
