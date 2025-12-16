package com.wd.api.controller;

import com.wd.api.dto.CustomerProjectCreateRequest;
import com.wd.api.dto.CustomerProjectResponse;
import com.wd.api.dto.CustomerProjectUpdateRequest;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.ProjectMember;
import com.wd.api.model.User;
import com.wd.api.dto.TeamMemberSelectionDTO;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.UserRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.repository.ProjectDocumentRepository;
import com.wd.api.model.Task;
import com.wd.api.model.ProjectDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/customer-projects")
public class CustomerProjectController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerProjectController.class);

    @Autowired
    private CustomerProjectRepository customerProjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.wd.api.repository.ProjectMemberRepository projectMemberRepository;

    @Autowired
    private com.wd.api.repository.PortalUserRepository portalUserRepository;

    @Autowired
    private com.wd.api.repository.CustomerUserRepository customerUserRepository;

    @Autowired
    private com.wd.api.repository.PortalRoleRepository portalRoleRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectDocumentRepository projectDocumentRepository;

    /**
     * Get all customer projects
     */
    @GetMapping
    public ResponseEntity<List<CustomerProjectResponse>> getAllCustomerProjects() {
        try {
            List<CustomerProject> projects = customerProjectRepository.findAll();
            List<CustomerProjectResponse> responses = projects.stream()
                    .map(CustomerProjectResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Error fetching customer projects", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get customer project by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerProjectById(@PathVariable Long id) {
        try {
            Optional<CustomerProject> projectOpt = customerProjectRepository.findById(id);
            if (!projectOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(new CustomerProjectResponse(projectOpt.get()));
        } catch (Exception e) {
            logger.error("Error fetching customer project with ID: {}", id, e);
            return ResponseEntity.internalServerError().body("Error fetching customer project");
        }
    }

    /**
     * Create customer project
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> createCustomerProject(@RequestBody CustomerProjectCreateRequest request) {
        try {
            // Validate required fields
            if (request == null) {
                return ResponseEntity.badRequest().body("Request body is required");
            }

            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Project name is required");
            }

            if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Location is required");
            }

            // Validate progress if provided
            if (request.getProgress() != null) {
                if (request.getProgress() < 0 || request.getProgress() > 100) {
                    return ResponseEntity.badRequest().body("Progress must be between 0 and 100");
                }
            }

            // Validate end date is after start date if both provided
            if (request.getStartDate() != null && request.getEndDate() != null) {
                if (request.getEndDate().isBefore(request.getStartDate())) {
                    return ResponseEntity.badRequest().body("End date must be after start date");
                }
            }

            // Get current logged-in user
            String createdBy = null;
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()
                        && !authentication.getName().equals("anonymousUser")) {
                    createdBy = authentication.getName();
                }
            } catch (Exception e) {
                logger.warn("Error getting current user for project creation", e);
            }

            // Generate unique project code
            String projectCode = generateUniqueProjectCode();

            // Create new project
            CustomerProject project = new CustomerProject();
            project.setName(request.getName().trim());
            project.setLocation(request.getLocation().trim());
            project.setStartDate(request.getStartDate());
            project.setEndDate(request.getEndDate());
            // Set progress to 0 if not provided
            if (request.getProgress() != null) {
                project.setProgress(request.getProgress());
            } else {
                project.setProgress(0.0);
            }
            project.setCreatedBy(createdBy);
            project.setProjectPhase(request.getProjectPhase() != null && !request.getProjectPhase().trim().isEmpty()
                    ? request.getProjectPhase().trim()
                    : "Planning");
            project.setState(request.getState() != null && !request.getState().trim().isEmpty()
                    ? request.getState().trim()
                    : "Kerala");
            project.setDistrict(request.getDistrict() != null && !request.getDistrict().trim().isEmpty()
                    ? request.getDistrict().trim()
                    : null);
            // Handle sqfeet - already converted to BigDecimal by @JsonSetter
            project.setSqfeet(request.getSqfeet());
            // Handle leadId - already converted to Long by @JsonSetter
            project.setLeadId(request.getLeadId());
            project.setCustomerId(request.getCustomerId());
            project.setCode(projectCode);

            // Handle team members
            // 1. Auto-assign Portal Admins
            assignPortalAdmins(project);

            // 2. Assign selected team members
            if (request.getTeamMembers() != null && !request.getTeamMembers().isEmpty()) {
                assignSelectedMembers(project, request.getTeamMembers());
            }

            CustomerProject savedProject = customerProjectRepository.save(project);
            return ResponseEntity.status(HttpStatus.CREATED).body(new CustomerProjectResponse(savedProject));

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error creating customer project: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating customer project", e);
            return ResponseEntity.internalServerError().body("Error creating customer project");
        }
    }

    /**
     * Update customer project
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> updateCustomerProject(@PathVariable Long id,
            @RequestBody CustomerProjectUpdateRequest request) {
        try {
            // Log incoming request for debugging
            logger.debug("UPDATE CUSTOMER PROJECT REQUEST - Project ID: {}", id);
            if (request != null) {
                logger.debug(
                        "Request details - name: {}, location: {}, code: {}, leadId: {}, sqfeet: {}, progress: {}, startDate: {}, endDate: {}, projectPhase: {}, state: {}, district: {}",
                        request.getName(), request.getLocation(), request.getCode(), request.getLeadId(),
                        request.getSqfeet(), request.getProgress(), request.getStartDate(), request.getEndDate(),
                        request.getProjectPhase(), request.getState(), request.getDistrict());
            } else {
                logger.warn("UPDATE CUSTOMER PROJECT REQUEST - Request body is NULL for project ID: {}", id);
            }

            // Validate ID
            if (id == null) {
                return ResponseEntity.badRequest().body("Project ID is required");
            }

            Optional<CustomerProject> projectOpt = customerProjectRepository.findById(id);
            if (!projectOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project with ID " + id + " not found");
            }

            CustomerProject project = projectOpt.get();
            logger.debug("Existing project - ID: {}, code: {}", project.getId(), project.getCode());

            // Validate required fields
            if (request == null) {
                logger.warn("UPDATE CUSTOMER PROJECT - Request body is null for project ID: {}", id);
                return ResponseEntity.badRequest().body("Request body is required");
            }

            logger.debug("Validating request fields for project ID: {}", id);
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                logger.warn("UPDATE CUSTOMER PROJECT - Project name is null or empty for project ID: {}", id);
                return ResponseEntity.badRequest().body("Project name is required");
            }
            logger.debug("Name validation passed: {}", request.getName());

            if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
                logger.warn("UPDATE CUSTOMER PROJECT - Location is null or empty for project ID: {}", id);
                return ResponseEntity.badRequest().body("Location is required");
            }
            logger.debug("Location validation passed: {}", request.getLocation());

            // Validate progress if provided
            if (request.getProgress() != null) {
                logger.debug("Validating progress: {}", request.getProgress());
                if (request.getProgress() < 0 || request.getProgress() > 100) {
                    logger.warn("UPDATE CUSTOMER PROJECT - Progress out of range: {} for project ID: {}",
                            request.getProgress(), id);
                    return ResponseEntity.badRequest().body("Progress must be between 0 and 100");
                }
                logger.debug("Progress validation passed");
            } else {
                logger.debug("Progress is null (optional field)");
            }

            // Validate end date is after start date if both provided
            if (request.getStartDate() != null && request.getEndDate() != null) {
                logger.debug("Validating dates - start: {}, end: {}", request.getStartDate(), request.getEndDate());
                if (request.getEndDate().isBefore(request.getStartDate())) {
                    logger.warn("UPDATE CUSTOMER PROJECT - End date is before start date for project ID: {}", id);
                    return ResponseEntity.badRequest().body("End date must be after start date");
                }
                logger.debug("Date validation passed");
            } else {
                logger.debug("Dates validation skipped (one or both are null)");
            }

            // Validate code uniqueness if code is being changed
            logger.debug("CODE VALIDATION - Project ID: {}", id);
            if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
                String newCode = request.getCode().trim();
                String existingCode = project.getCode();
                logger.debug("Code validation - new: '{}' (length: {}), existing: '{}' (length: {})",
                        newCode, newCode.length(), existingCode, existingCode != null ? existingCode.length() : 0);

                // Normalize codes for comparison (trim and handle null)
                String normalizedNewCode = newCode.trim();
                String normalizedExistingCode = (existingCode != null) ? existingCode.trim() : null;

                // Only check uniqueness if code is actually being changed
                boolean codeChanged = (normalizedExistingCode == null
                        || !normalizedNewCode.equals(normalizedExistingCode));
                logger.debug("Code changed: {}, normalized new: '{}', normalized existing: '{}'",
                        codeChanged, normalizedNewCode, normalizedExistingCode);

                if (codeChanged) {
                    logger.debug("Code is being changed, checking uniqueness for: '{}'", normalizedNewCode);
                    Optional<CustomerProject> existingProject = customerProjectRepository.findByCode(normalizedNewCode);
                    if (existingProject.isPresent()) {
                        CustomerProject foundProject = existingProject.get();
                        Long existingProjectId = foundProject.getId();
                        String foundProjectCode = foundProject.getCode();
                        logger.debug("Found project with code '{}' - ID: {}, code: '{}'",
                                normalizedNewCode, existingProjectId, foundProjectCode);

                        // Only fail if it's a different project
                        // Use Objects.equals for null-safe comparison
                        if (!java.util.Objects.equals(existingProjectId, id)) {
                            logger.warn(
                                    "UPDATE CUSTOMER PROJECT - Code conflict! Code '{}' belongs to project ID {}, not {}",
                                    normalizedNewCode, existingProjectId, id);
                            return ResponseEntity.badRequest().body("Project code '" + normalizedNewCode
                                    + "' already exists for project ID " + existingProjectId);
                        } else {
                            logger.debug("Code belongs to same project (ID {}), update allowed", id);
                        }
                    } else {
                        logger.debug("No existing project with code '{}', update allowed", normalizedNewCode);
                    }
                } else {
                    logger.debug("Code unchanged (both are '{}'), skipping uniqueness check", normalizedNewCode);
                }
            } else {
                logger.debug("Code is null or empty in request, keeping existing code: '{}'", project.getCode());
            }

            // Update fields
            project.setName(request.getName().trim());
            project.setLocation(request.getLocation().trim());
            project.setStartDate(request.getStartDate());
            project.setEndDate(request.getEndDate());
            // Handle progress - can be null
            if (request.getProgress() != null) {
                project.setProgress(request.getProgress());
            } else {
                project.setProgress(null);
            }
            // Don't update createdBy - it should remain as original creator
            project.setProjectPhase(request.getProjectPhase() != null && !request.getProjectPhase().trim().isEmpty()
                    ? request.getProjectPhase().trim()
                    : null);
            project.setState(request.getState() != null && !request.getState().trim().isEmpty()
                    ? request.getState().trim()
                    : null);
            project.setDistrict(request.getDistrict() != null && !request.getDistrict().trim().isEmpty()
                    ? request.getDistrict().trim()
                    : null);
            // Handle sqfeet - already converted to BigDecimal by @JsonSetter
            project.setSqfeet(request.getSqfeet());

            // Handle leadId - already converted to Long by @JsonSetter
            project.setLeadId(request.getLeadId());
            project.setCustomerId(request.getCustomerId());

            // Handle team members
            if (request.getTeamMembers() != null) {
                // Clear existing project members (except maybe admins? User didn't specify, but
                // usually update replaces selection)
                // However, the requirement says "add all portal_users who's role is admin".
                // If we clear, we should re-add admins or just ensure they are there.
                // For simplicity, let's clear and re-process everything including auto-admins
                // to be safe.
                project.getProjectMembers().clear();

                // 1. Auto-assign Portal Admins (ensure they are always there)
                assignPortalAdmins(project);

                // 2. Assign selected team members
                assignSelectedMembers(project, request.getTeamMembers());
            }

            // Allow code to be updated in edit screen
            // Note: Code uniqueness was already validated above
            if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
                String newCode = request.getCode().trim();
                String currentCode = project.getCode();
                logger.debug("Updating code - new: '{}', current: '{}' for project ID: {}", newCode, currentCode, id);
                // Update the code (uniqueness already validated)
                project.setCode(newCode);
                logger.debug("Code updated to: '{}' for project ID: {}", project.getCode(), id);
            } else if (request.getCode() != null && request.getCode().trim().isEmpty()) {
                // If empty string is sent, set to null
                logger.debug("Empty code provided, setting to null for project ID: {}", id);
                project.setCode(null);
            } else {
                // If code is null in request, keep existing code (don't change it)
                logger.debug("Code not provided in request, keeping existing: '{}' for project ID: {}",
                        project.getCode(), id);
            }

            // Validate entity state before save - ensure required fields are set
            logger.debug("Validating entity state before save for project ID: {}", id);
            if (project.getName() == null || project.getName().trim().isEmpty()) {
                logger.error("UPDATE CUSTOMER PROJECT - Project name is null or empty for project ID: {}", id);
                return ResponseEntity.badRequest().body("Project name cannot be empty after update");
            }
            logger.debug("Name is valid: {}", project.getName());

            if (project.getLocation() == null || project.getLocation().trim().isEmpty()) {
                logger.error("UPDATE CUSTOMER PROJECT - Project location is null or empty for project ID: {}", id);
                return ResponseEntity.badRequest().body("Location cannot be empty after update");
            }
            logger.debug("Location is valid: {}", project.getLocation());

            // Ensure name and location are trimmed
            if (!project.getName().equals(project.getName().trim())) {
                project.setName(project.getName().trim());
            }
            if (!project.getLocation().equals(project.getLocation().trim())) {
                project.setLocation(project.getLocation().trim());
            }
            logger.debug("Entity validation passed for project ID: {}", id);

            // Log the project state before save for debugging
            logger.debug(
                    "BEFORE SAVE - Project ID: {}, name: {}, location: {}, code: {}, leadId: {}, sqfeet: {}, progress: {}, state: {}, district: {}, phase: {}, startDate: {}, endDate: {}, createdBy: {}, createdAt: {}, updatedAt: {}",
                    id, project.getName(), project.getLocation(), project.getCode(), project.getLeadId(),
                    project.getSqfeet(), project.getProgress(), project.getState(), project.getDistrict(),
                    project.getProjectPhase(), project.getStartDate(), project.getEndDate(),
                    project.getCreatedBy(), project.getCreatedAt(), project.getUpdatedAt());

            try {
                logger.debug("Attempting to save project ID: {}", id);
                CustomerProject updatedProject = customerProjectRepository.save(project);
                logger.info("Customer project updated successfully - ID: {}", updatedProject.getId());

                // Create response
                try {
                    CustomerProjectResponse response = new CustomerProjectResponse(updatedProject);
                    logger.debug("Response created successfully for project ID: {}", id);
                    return ResponseEntity.ok(response);
                } catch (Exception e) {
                    logger.error("Error creating response for project ID: {}", id, e);
                    // Still return success but with error in response creation
                    return ResponseEntity.ok().body("Project updated successfully but error creating response");
                }
            } catch (org.hibernate.exception.ConstraintViolationException e) {
                logger.error("Hibernate constraint violation saving project ID: {} - SQL State: {}, Error Code: {}",
                        id, e.getSQLState(), e.getErrorCode(), e);
                return ResponseEntity.badRequest().body("Database constraint violation: " +
                        (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            } catch (org.hibernate.exception.DataException e) {
                logger.error("Hibernate data exception saving project ID: {} - SQL State: {}",
                        id, e.getSQLState(), e);
                return ResponseEntity.badRequest().body("Data error: " +
                        (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            } catch (org.hibernate.exception.SQLGrammarException e) {
                logger.error("Hibernate SQL grammar exception saving project ID: {} - SQL: {}",
                        id, e.getSQL(), e);
                return ResponseEntity.internalServerError().body("SQL error: " +
                        (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            } catch (Exception e) {
                logger.error("Unexpected error during save for project ID: {} - Exception type: {}",
                        id, e.getClass().getName(), e);
                throw e; // Re-throw to be caught by outer catch block
            }

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            logger.error("Data integrity violation updating customer project ID: {} - Root cause: {}",
                    id, e.getRootCause() != null ? e.getRootCause().getMessage() : "null", e);
            String errorMsg = e.getMessage();
            String rootCause = e.getRootCause() != null ? e.getRootCause().getMessage() : "";
            if (errorMsg != null && (errorMsg.contains("code") || rootCause.contains("code"))) {
                return ResponseEntity.badRequest()
                        .body("Project code already exists or violates constraint: " + rootCause);
            } else if (errorMsg != null && (errorMsg.contains("lead_id") || rootCause.contains("lead_id"))) {
                return ResponseEntity.badRequest().body("Invalid lead ID: " + rootCause);
            } else if (errorMsg != null && (errorMsg.contains("null") || rootCause.contains("null"))) {
                return ResponseEntity.badRequest().body("Required field is null: " + rootCause);
            } else {
                return ResponseEntity.badRequest().body("Data integrity violation: " + errorMsg +
                        (rootCause != null && !rootCause.isEmpty() ? " - Root cause: " + rootCause : ""));
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error updating customer project ID: {} - {}", id, e.getMessage());
            return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
        } catch (jakarta.validation.ConstraintViolationException e) {
            logger.warn("Constraint violation updating customer project ID: {}", id, e);
            StringBuilder violations = new StringBuilder("Constraint violations: ");
            e.getConstraintViolations().forEach(v -> {
                violations.append(v.getPropertyPath()).append(": ").append(v.getMessage()).append("; ");
            });
            return ResponseEntity.badRequest().body(violations.toString());
        } catch (Exception e) {
            logger.error("Error updating customer project ID: {}", id, e);
            // If it's a 400-level error, return bad request, otherwise 500
            if (e instanceof IllegalArgumentException || e instanceof jakarta.validation.ConstraintViolationException) {
                return ResponseEntity.badRequest().body("Validation error: " + e.getMessage());
            }
            return ResponseEntity.internalServerError().body("Error updating customer project");
        }
    }

    /**
     * Delete customer project
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCustomerProject(@PathVariable Long id) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body("Project ID is required");
            }

            Optional<CustomerProject> projectOpt = customerProjectRepository.findById(id);
            if (!projectOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            // Manual Cascade Delete for Dependents
            // 1. Delete Tasks
            List<Task> tasks = taskRepository.findByProjectId(id);
            if (!tasks.isEmpty()) {
                logger.info("Deleting {} tasks for project ID: {}", tasks.size(), id);
                taskRepository.deleteAll(tasks);
            }

            // 2. Delete Documents
            List<ProjectDocument> docs = projectDocumentRepository.findAllByProjectId(id);
            if (!docs.isEmpty()) {
                logger.info("Deleting {} documents for project ID: {}", docs.size(), id);
                projectDocumentRepository.deleteAll(docs);
            }

            // 3. Project Members are handled by CascadeType.ALL in Entity

            customerProjectRepository.deleteById(id);
            logger.info("Customer project deleted successfully - ID: {}", id);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error deleting customer project with ID: {}", id, e);
            return ResponseEntity.internalServerError().body("Error deleting customer project: " + e.getMessage());
        }
    }

    /**
     * Get customer projects by lead ID
     */
    @GetMapping("/lead/{leadId}")
    public ResponseEntity<List<CustomerProjectResponse>> getCustomerProjectsByLeadId(@PathVariable Long leadId) {
        try {
            List<CustomerProject> projects = customerProjectRepository.findByLeadId(leadId);
            List<CustomerProjectResponse> responses = projects.stream()
                    .map(CustomerProjectResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Error fetching customer projects by lead ID: {}", leadId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private void assignPortalAdmins(CustomerProject project) {
        try {
            Optional<com.wd.api.model.PortalRole> adminRoleOpt = portalRoleRepository.findByCodeIgnoreCase("ADMIN");
            if (adminRoleOpt.isPresent()) {
                List<com.wd.api.model.PortalUser> admins = portalUserRepository
                        .findByRoleId(adminRoleOpt.get().getId());
                for (com.wd.api.model.PortalUser admin : admins) {
                    // Check if already assigned
                    boolean alreadyAssigned = project.getProjectMembers().stream()
                            .anyMatch(pm -> pm.getPortalUser() != null
                                    && pm.getPortalUser().getId().equals(admin.getId()));

                    if (!alreadyAssigned) {
                        ProjectMember member = new ProjectMember();
                        member.setProject(project);
                        member.setPortalUser(admin);
                        member.setRoleInProject("ADMIN");
                        project.getProjectMembers().add(member);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error auto-assigning admins", e);
        }
    }

    private void assignSelectedMembers(CustomerProject project, List<TeamMemberSelectionDTO> selectedMembers) {
        for (TeamMemberSelectionDTO selection : selectedMembers) {
            if (selection.getId() == null || selection.getType() == null)
                continue;

            if ("PORTAL".equalsIgnoreCase(selection.getType())) {
                // Check if already assigned (e.g. via admin auto-assign)
                boolean alreadyAssigned = project.getProjectMembers().stream()
                        .anyMatch(pm -> pm.getPortalUser() != null
                                && pm.getPortalUser().getId().equals(selection.getId()));

                if (!alreadyAssigned) {
                    Optional<com.wd.api.model.PortalUser> userOpt = portalUserRepository.findById(selection.getId());
                    if (userOpt.isPresent()) {
                        ProjectMember member = new ProjectMember();
                        member.setProject(project);
                        member.setPortalUser(userOpt.get());
                        member.setRoleInProject("MEMBER");
                        project.getProjectMembers().add(member);
                    }
                }
            } else if ("CUSTOMER".equalsIgnoreCase(selection.getType())) {
                // Check if already assigned
                boolean alreadyAssigned = project.getProjectMembers().stream()
                        .anyMatch(pm -> pm.getCustomerUser() != null
                                && pm.getCustomerUser().getId().equals(selection.getId()));

                if (!alreadyAssigned) {
                    Optional<com.wd.api.model.CustomerUser> userOpt = customerUserRepository
                            .findById(selection.getId());
                    if (userOpt.isPresent()) {
                        ProjectMember member = new ProjectMember();
                        member.setProject(project);
                        member.setCustomerUser(userOpt.get());
                        member.setRoleInProject("MEMBER");
                        project.getProjectMembers().add(member);
                    }
                }
            }
        }
    }

    /**
     * Generate unique project code
     * Format: PROJ-{short UUID}
     */
    private String generateUniqueProjectCode() {
        String code;
        int attempts = 0;
        do {
            // Generate short UUID (first 8 characters)
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            code = "PROJ-" + uuid;
            attempts++;
            if (attempts > 10) {
                // Fallback to timestamp-based code if UUID collision (unlikely)
                code = "PROJ-" + System.currentTimeMillis() % 100000000;
                break;
            }
        } while (customerProjectRepository.findByCode(code).isPresent());

        return code;
    }
}
