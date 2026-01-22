package com.wd.api.controller;

import com.wd.api.model.Lead;
import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.LeadSearchFilter;
import com.wd.api.dto.ActivityFeedDTO;
import com.wd.api.service.LeadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Lead management operations
 * Enterprise-grade implementation with comprehensive CRUD and search capabilities
 */
@RestController
@RequestMapping("/leads")
public class LeadController {

    private static final Logger logger = LoggerFactory.getLogger(LeadController.class);

    @Autowired
    private LeadService leadService;

    /**
     * Get paginated leads with filtering and sorting
     * Uses modern LeadSearchFilter (non-deprecated) for enterprise-grade search
     * Supports both legacy and modern parameter formats for backward compatibility
     * 
     * Query Parameters:
     * - page (int, default 0): Page number (0-based)
     * - size (int, default 20): Page size (max 100)
     * - limit (int, optional): Legacy parameter, maps to size
     * - sortBy (string, default "id"): Field to sort by
     * - sortOrder/sortDirection (string, default "desc"): Sort direction (asc/desc)
     * - status (string, optional): Filter by lead status
     * - source (string, optional): Filter by lead source
     * - priority (string, optional): Filter by priority
     * - customerType (string, optional): Filter by customer type
     * - projectType (string, optional): Filter by project type
     * - assignedTeam (string, optional): Filter by assigned team/user
     * - state (string, optional): Filter by state
     * - district (string, optional): Filter by district
     * - minBudget (double, optional): Minimum budget filter
     * - maxBudget (double, optional): Maximum budget filter
     * - startDate (date, optional): Start date for created_at filter
     * - endDate (date, optional): End date for created_at filter
     * - search (string, optional): Search query across multiple fields
     * 
     * @param filter Search and filter parameters
     * @return Paginated list of leads
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<Page<Lead>>> getLeadsPaginated(
            @ModelAttribute LeadSearchFilter filter,
            // Legacy parameter support for backward compatibility
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sortOrder) {
        try {
            // Handle legacy parameters (PaginationParams format)
            // Convert 1-based page to 0-based if legacy format is used
            if (page != null) {
                // Legacy: page=0 or page=1 both mean first page
                // Modern: page=0 is first page (0-based)
                if (page == 0 || page == 1) {
                    filter.setPage(0);
                } else if (page > 1) {
                    filter.setPage(page - 1); // Convert 1-based to 0-based
                }
            }
            
            // Handle legacy limit parameter
            if (limit != null && limit > 0) {
                filter.setSize(Math.min(limit, 100)); // Cap at 100 for safety
            }
            
            // Handle legacy sortOrder parameter
            if (sortOrder != null && filter.getSortDirection() == null) {
                filter.setSortDirection(sortOrder.toLowerCase());
            }
            
            // Normalize sortBy field name (handle both snake_case and camelCase)
            if (filter.getSortBy() != null) {
                String sortBy = filter.getSortBy();
                // Convert common snake_case to camelCase for entity fields
                switch (sortBy.toLowerCase()) {
                    case "created_at":
                        filter.setSortBy("createdAt");
                        break;
                    case "updated_at":
                        filter.setSortBy("updatedAt");
                        break;
                    case "lead_status":
                        filter.setSortBy("leadStatus");
                        break;
                    case "assigned_to":
                        filter.setSortBy("assignedTo");
                        break;
                    case "date_of_enquiry":
                        filter.setSortBy("dateOfEnquiry");
                        break;
                    case "lead_source":
                        filter.setSortBy("leadSource");
                        break;
                    case "customer_type":
                        filter.setSortBy("customerType");
                        break;
                    case "project_type":
                        filter.setSortBy("projectType");
                        break;
                    case "next_follow_up":
                        filter.setSortBy("nextFollowUp");
                        break;
                    case "last_contact_date":
                        filter.setSortBy("lastContactDate");
                        break;
                    // If already camelCase, keep as is
                }
            } else {
                filter.setSortBy("createdAt"); // Default sort
            }
            
            // Normalize sortDirection (handle case variations)
            if (filter.getSortDirection() != null) {
                String direction = filter.getSortDirection().toLowerCase();
                if (!"asc".equals(direction) && !"desc".equals(direction)) {
                    filter.setSortDirection("desc"); // Default to desc if invalid
                } else {
                    filter.setSortDirection(direction);
                }
            } else {
                filter.setSortDirection("desc");
            }

            // Use non-deprecated search method
            Page<Lead> leads = leadService.search(filter);
            return ResponseEntity.ok(ApiResponse.success("Leads retrieved successfully", leads));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid pagination parameters: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid pagination parameters: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching paginated leads", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve leads: " + e.getMessage()));
        }
    }

    /**
     * Get activities for a specific lead
     * Returns combined activities from activity_feeds and lead_interactions tables
     * 
     * @param leadId The ID of the lead
     * @return List of activities sorted by date (most recent first)
     */
    @GetMapping("/{leadId}/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<ActivityFeedDTO>>> getLeadActivities(@PathVariable Long leadId) {
        try {
            List<ActivityFeedDTO> activities = leadService.getLeadActivities(leadId);
            return ResponseEntity.ok(ApiResponse.success("Lead activities retrieved successfully", activities));
        } catch (IllegalArgumentException e) {
            logger.warn("Lead not found: {}", leadId);
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Lead not found with id: " + leadId));
        } catch (Exception e) {
            logger.error("Error fetching activities for lead {}: {}", leadId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve lead activities: " + e.getMessage()));
        }
    }
}
