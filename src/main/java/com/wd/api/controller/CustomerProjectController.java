package com.wd.api.controller;

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
    public ResponseEntity<?> getAllCustomerProjects(
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

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Error fetching customer projects", e);
            return ResponseEntity.internalServerError().body("Error fetching projects: " + e.getMessage());
        }
    }

    /**
     * Get customer project by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerProjectById(@PathVariable Long id) {
        try {
            return customerProjectService.getProjectById(id)
                    .map(project -> ResponseEntity.ok(new CustomerProjectResponse(project)))
                    .orElse(ResponseEntity.notFound().build());
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
            // Get current logged-in user
            String createdBy = getCurrentUsername();

            CustomerProject savedProject = customerProjectService.createProject(request, createdBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(new CustomerProjectResponse(savedProject));

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error creating customer project: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
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
            CustomerProject updatedProject = customerProjectService.updateProject(id, request);
            return ResponseEntity.ok(new CustomerProjectResponse(updatedProject));

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error updating project ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating customer project with ID: {}", id, e);
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
            customerProjectService.deleteProject(id);
            logger.info("Customer project deleted successfully - ID: {}", id);
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            logger.warn("Project not found for deletion: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting customer project with ID: {}", id, e);
            return ResponseEntity.internalServerError().body("Error deleting customer project: " + e.getMessage());
        }
    }

    /**
     * Get customer projects by lead ID
     */
    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<?> getCustomerProjectsByLeadId(@PathVariable Long leadId) {
        try {
            List<CustomerProject> projects = customerProjectService.getProjectsByLeadId(leadId);
            List<CustomerProjectResponse> responses = projects.stream()
                    .map(CustomerProjectResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Error fetching projects for lead ID: {}", leadId, e);
            return ResponseEntity.internalServerError().body("Error fetching projects");
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
