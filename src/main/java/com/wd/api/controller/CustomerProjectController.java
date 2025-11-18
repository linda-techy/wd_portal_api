package com.wd.api.controller;

import com.wd.api.dto.CustomerProjectCreateRequest;
import com.wd.api.dto.CustomerProjectResponse;
import com.wd.api.dto.CustomerProjectUpdateRequest;
import com.wd.api.model.CustomerProject;
import com.wd.api.repository.CustomerProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    
    @Autowired
    private CustomerProjectRepository customerProjectRepository;
    
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
            System.err.println("Error fetching customer projects: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Error fetching customer project: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error fetching customer project: " + e.getMessage());
        }
    }
    
    /**
     * Create customer project
     */
    @PostMapping
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
                System.err.println("Error getting current user: " + e.getMessage());
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
            project.setProgress(request.getProgress() != null ? request.getProgress() : 0.0);
            project.setCreatedBy(createdBy);
            project.setProjectPhase(request.getProjectPhase() != null && !request.getProjectPhase().trim().isEmpty()
                    ? request.getProjectPhase().trim() : "Planning");
            project.setState(request.getState() != null && !request.getState().trim().isEmpty()
                    ? request.getState().trim() : "Kerala");
            project.setDistrict(request.getDistrict() != null && !request.getDistrict().trim().isEmpty()
                    ? request.getDistrict().trim() : null);
            project.setSqfeet(request.getSqfeet());
            project.setLeadId(request.getLeadId());
            project.setCode(projectCode);
            
            CustomerProject savedProject = customerProjectRepository.save(project);
            return ResponseEntity.status(HttpStatus.CREATED).body(new CustomerProjectResponse(savedProject));
            
        } catch (Exception e) {
            System.err.println("Error creating customer project: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error creating customer project: " + e.getMessage());
        }
    }
    
    /**
     * Update customer project
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomerProject(@PathVariable Long id, @RequestBody CustomerProjectUpdateRequest request) {
        try {
            // Validate ID
            if (id == null) {
                return ResponseEntity.badRequest().body("Project ID is required");
            }
            
            Optional<CustomerProject> projectOpt = customerProjectRepository.findById(id);
            if (!projectOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            CustomerProject project = projectOpt.get();
            
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
            
            // Validate code uniqueness if code is being changed
            if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
                String newCode = request.getCode().trim();
                if (!newCode.equals(project.getCode())) {
                    Optional<CustomerProject> existingProject = customerProjectRepository.findByCode(newCode);
                    if (existingProject.isPresent() && !existingProject.get().getId().equals(id)) {
                        return ResponseEntity.badRequest().body("Project code already exists");
                    }
                }
            }
            
            // Update fields
            project.setName(request.getName().trim());
            project.setLocation(request.getLocation().trim());
            project.setStartDate(request.getStartDate());
            project.setEndDate(request.getEndDate());
            project.setProgress(request.getProgress());
            // Don't update createdBy - it should remain as original creator
            project.setProjectPhase(request.getProjectPhase() != null && !request.getProjectPhase().trim().isEmpty()
                    ? request.getProjectPhase().trim() : null);
            project.setState(request.getState() != null && !request.getState().trim().isEmpty()
                    ? request.getState().trim() : null);
            project.setDistrict(request.getDistrict() != null && !request.getDistrict().trim().isEmpty()
                    ? request.getDistrict().trim() : null);
            project.setSqfeet(request.getSqfeet());
            project.setLeadId(request.getLeadId());
            // Allow code to be updated in edit screen
            if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
                project.setCode(request.getCode().trim());
            }
            
            CustomerProject updatedProject = customerProjectRepository.save(project);
            return ResponseEntity.ok(new CustomerProjectResponse(updatedProject));
            
        } catch (Exception e) {
            System.err.println("Error updating customer project: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error updating customer project: " + e.getMessage());
        }
    }
    
    /**
     * Delete customer project
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCustomerProject(@PathVariable Long id) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body("Project ID is required");
            }
            
            Optional<CustomerProject> projectOpt = customerProjectRepository.findById(id);
            if (!projectOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            customerProjectRepository.deleteById(id);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            System.err.println("Error deleting customer project: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Error fetching customer projects by lead ID: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
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

