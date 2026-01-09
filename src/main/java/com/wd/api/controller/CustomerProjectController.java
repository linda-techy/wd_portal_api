package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.CustomerProjectCreateRequest;
import com.wd.api.dto.CustomerProjectResponse;
import com.wd.api.dto.CustomerProjectUpdateRequest;
import com.wd.api.model.CustomerProject;
import com.wd.api.service.CustomerProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Customer Project operations
 * Delegates all business logic to CustomerProjectService
 */
@RestController
@RequestMapping("/customer-projects")
public class CustomerProjectController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerProjectController.class);

    @Autowired
    private CustomerProjectService customerProjectService;

    /**
     * Get all customer projects with support for pagination and search
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerProjectResponse>>> getAllCustomerProjects(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {
        try {
            // Create Sort object
            Sort sortObj = Sort.by(sort[0]);
            if (sort.length > 1 && sort[1].equalsIgnoreCase("asc")) {
                sortObj = sortObj.ascending();
            } else {
                sortObj = sortObj.descending();
            }

            Pageable pageable = PageRequest.of(page, size, sortObj);
            Page<CustomerProjectResponse> responses = customerProjectService.getAllProjects(search, pageable);

            return ResponseEntity.ok(ApiResponse.success("Projects retrieved successfully", responses));
        } catch (Exception e) {
            logger.error("Error fetching customer projects", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Error fetching projects: " + e.getMessage()));
        }
    }

    /**
     * Get customer project by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerProjectResponse>> getCustomerProjectById(@PathVariable Long id) {
        try {
            return customerProjectService.getProjectById(id)
                    .map(project -> ResponseEntity.ok(ApiResponse.success("Project retrieved successfully",
                            new CustomerProjectResponse(project))))
                    .orElse(ResponseEntity.status(404).body(ApiResponse.error("Project not found")));
        } catch (Exception e) {
            logger.error("Error fetching customer project with ID: {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Create customer project
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<CustomerProjectResponse>> createCustomerProject(
            @RequestBody CustomerProjectCreateRequest request) {
        try {
            // Get current logged-in user
            String createdBy = getCurrentUsername();

            CustomerProject savedProject = customerProjectService.createProject(request, createdBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success("Project created successfully", new CustomerProjectResponse(savedProject)));

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error creating customer project: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating customer project", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Update customer project
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<CustomerProjectResponse>> updateCustomerProject(@PathVariable Long id,
            @RequestBody CustomerProjectUpdateRequest request) {
        try {
            CustomerProject updatedProject = customerProjectService.updateProject(id, request);
            return ResponseEntity.ok(
                    ApiResponse.success("Project updated successfully", new CustomerProjectResponse(updatedProject)));

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error updating project ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating customer project with ID: {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Delete customer project
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCustomerProject(@PathVariable Long id) {
        try {
            customerProjectService.deleteProject(id);
            logger.info("Customer project deleted successfully - ID: {}", id);
            return ResponseEntity.ok(ApiResponse.success("Project deleted successfully"));

        } catch (IllegalArgumentException e) {
            logger.warn("Project not found for deletion: {}", id);
            return ResponseEntity.status(404).body(ApiResponse.error("Project not found"));
        } catch (IllegalStateException e) {
            logger.warn("Cannot delete project ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting customer project with ID: {}", id, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error deleting customer project: " + e.getMessage()));
        }
    }

    /**
     * Get customer projects by lead ID
     */
    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<ApiResponse<List<CustomerProjectResponse>>> getCustomerProjectsByLeadId(
            @PathVariable Long leadId) {
        try {
            List<CustomerProject> projects = customerProjectService.getProjectsByLeadId(leadId);
            List<CustomerProjectResponse> responses = projects.stream()
                    .map(CustomerProjectResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success("Lead projects retrieved successfully", responses));
        } catch (Exception e) {
            logger.error("Error fetching projects for lead ID: {}", leadId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * Get current logged-in username
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !authentication.getName().equals("anonymousUser")) {
                return authentication.getName();
            }
        } catch (Exception e) {
            logger.warn("Error getting current user for project creation", e);
        }
        return null;
    }
}
