package com.wd.api.service;

import com.wd.api.dto.CustomerProjectCreateRequest;
import com.wd.api.dto.CustomerProjectResponse;
import com.wd.api.dto.CustomerProjectUpdateRequest;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.model.ProjectDocument;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.CustomerUserRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.repository.ProjectDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private TaskRepository taskRepository;

    @Autowired
    private ProjectDocumentRepository projectDocumentRepository;

    /**
     * Get all projects with pagination and search
     */
    @Transactional(readOnly = true)
    public Page<CustomerProjectResponse> getAllProjects(String search, Pageable pageable) {
        Page<CustomerProject> projectPage;

        if (search != null && !search.trim().isEmpty()) {
            String searchPattern = search.trim();
            projectPage = customerProjectRepository
                    .findByNameContainingIgnoreCaseOrLocationContainingIgnoreCaseOrStateContainingIgnoreCaseOrProjectPhaseContainingIgnoreCase(
                            searchPattern, searchPattern, searchPattern, searchPattern, pageable);
        } else {
            projectPage = customerProjectRepository.findAll(pageable);
        }

        return projectPage.map(CustomerProjectResponse::new);
    }

    /**
     * Get project by ID
     */
    @Transactional(readOnly = true)
    public Optional<CustomerProject> getProjectById(Long id) {
        return customerProjectRepository.findById(id);
    }

    /**
     * Get projects by lead ID
     */
    @Transactional(readOnly = true)
    public List<CustomerProject> getProjectsByLeadId(Long leadId) {
        return customerProjectRepository.findByLeadId(leadId);
    }

    /**
     * Create new customer project
     */
    public CustomerProject createProject(CustomerProjectCreateRequest request, String createdBy) {
        // Validate required fields
        validateProjectRequest(request);

        // Generate unique project code
        String projectCode = generateUniqueProjectCode();

        // Create new project
        CustomerProject project = new CustomerProject();
        project.setName(request.getName().trim());
        project.setLocation(request.getLocation().trim());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setCreatedBy(createdBy);

        // Set defaults
        project.setProjectPhase(request.getProjectPhase() != null && !request.getProjectPhase().trim().isEmpty()
                ? request.getProjectPhase().trim()
                : "Planning");
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
        project.setCode(projectCode);

        // Set customer if provided
        if (request.getCustomerId() != null) {
            customerUserRepository.findById(request.getCustomerId())
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

        // Note: Team member assignment removed for simplification
        // Can be added back with proper DTO/entity method alignment

        return customerProjectRepository.save(project);
    }

    /**
     * Update existing customer project
     */
    public CustomerProject updateProject(Long id, CustomerProjectUpdateRequest request) {
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
        project.setProjectPhase(request.getProjectPhase() != null && !request.getProjectPhase().trim().isEmpty()
                ? request.getProjectPhase().trim()
                : null);
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

        // Update code if provided
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            project.setCode(request.getCode().trim());
        }

        return customerProjectRepository.save(project);
    }

    /**
     * Delete customer project with cascade delete of dependent entities
     */
    public void deleteProject(Long id) {
        CustomerProject project = customerProjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project with ID " + id + " not found"));

        // Manual cascade delete for dependent entities
        // Note: This should be replaced with database FK CASCADE in future

        // Delete tasks
        List<Task> tasks = taskRepository.findByProjectId(id);
        if (!tasks.isEmpty()) {
            logger.info("Deleting {} tasks for project ID: {}", tasks.size(), id);
            taskRepository.deleteAll(tasks);
        }

        // Delete documents
        List<ProjectDocument> docs = projectDocumentRepository.findAllByProjectId(id);
        if (!docs.isEmpty()) {
            logger.info("Deleting {} documents for project ID: {}", docs.size(), id);
            projectDocumentRepository.deleteAll(docs);
        }

        // Project members are handled by CascadeType.ALL in Entity
        customerProjectRepository.delete(project);
        logger.info("Customer project deleted successfully - ID: {}", id);
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

    private String generateUniqueProjectCode() {
        String code;
        do {
            String shortUuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            code = "PROJ-" + shortUuid;
        } while (customerProjectRepository.findByCode(code).isPresent());

        return code;
    }
}
