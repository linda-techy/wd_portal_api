package com.wd.api.dao.interfaces;

import com.wd.api.dao.model.Leads;
import com.wd.api.dto.PaginatedResponse;
import com.wd.api.dto.PaginationParams;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ILeadsDAO {
    
    // =====================================================
    // LEAD CRUD OPERATIONS
    // =====================================================
    
    /**
     * Get all leads from the database
     * @return List of all leads
     */
    List<Leads> getAllLeads();
    
    /**
     * Get paginated leads with filtering and sorting
     * @param params Pagination and filter parameters
     * @return PaginatedResponse containing leads and pagination info
     */
    PaginatedResponse<Leads> getLeadsPaginated(PaginationParams params);
    
    /**
     * Get a specific lead by ID
     * @param leadId Lead ID
     * @return Lead object or null if not found
     */
    Leads getLeadById(String leadId);
    
    /**
     * Create a new lead
     * @param lead Lead object to create
     * @return Number of rows affected (should be 1 if successful)
     */
    int createLead(Leads lead);
    
    /**
     * Update an existing lead
     * @param lead Lead object with updated data
     * @return Number of rows affected (should be 1 if successful)
     */
    int updateLead(Leads lead);
    
    /**
     * Delete a lead by ID
     * @param leadId Lead ID to delete
     * @return Number of rows affected (should be 1 if successful)
     */
    int deleteLead(String leadId);
    
    // =====================================================
    // LEAD FILTERING & SEARCH
    // =====================================================
    
    /**
     * Get leads by status
     * @param status Lead status to filter by
     * @return List of leads with the specified status
     */
    List<Leads> getLeadsByStatus(String status);
    
    /**
     * Get leads assigned to a specific team member
     * @param assignedTo Team member ID
     * @return List of leads assigned to the team member
     */
    List<Leads> getLeadsByAssignedTo(UUID assignedTo);
    
    /**
     * Get leads by source
     * @param source Lead source to filter by
     * @return List of leads from the specified source
     */
    List<Leads> getLeadsBySource(String source);
    
    /**
     * Get leads by priority
     * @param priority Lead priority to filter by
     * @return List of leads with the specified priority
     */
    List<Leads> getLeadsByPriority(String priority);
    
    /**
     * Get leads created within a date range
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of leads created within the date range
     */
    List<Leads> getLeadsByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * Search leads by various fields
     * @param searchTerm Search term to look for in name, email, phone, project_description, etc.
     * @return List of leads matching the search criteria
     */
    List<Leads> searchLeads(String searchTerm);
    
    // =====================================================
    // LEAD MANAGEMENT OPERATIONS
    // =====================================================
    
    /**
     * Assign a lead to a team member
     * @param leadId Lead ID
     * @param teamMemberId Team member ID
     * @return Number of rows affected (should be 1 if successful)
     */
    int assignLeadToTeamMember(String leadId, UUID teamMemberId);
    
    /**
     * Update lead status
     * @param leadId Lead ID
     * @param status New status
     * @return Number of rows affected (should be 1 if successful)
     */
    int updateLeadStatus(String leadId, String status);
    
    /**
     * Update lead priority
     * @param leadId Lead ID
     * @param priority New priority
     * @return Number of rows affected (should be 1 if successful)
     */
    int updateLeadPriority(String leadId, String priority);
    
    /**
     * Mark a lead as converted
     * @param leadId Lead ID
     * @return Number of rows affected (should be 1 if successful)
     */
    int markLeadAsConverted(String leadId);
    
    /**
     * Mark a lead as lost with a reason
     * @param leadId Lead ID
     * @param reason Reason for losing the lead
     * @return Number of rows affected (should be 1 if successful)
     */
    int markLeadAsLost(String leadId, String reason);
    
    /**
     * Get leads with overdue follow-ups
     * @return List of leads with overdue follow-ups
     */
    List<Leads> getOverdueFollowUps();
    
    // =====================================================
    // ANALYTICS & REPORTS
    // =====================================================
    
    /**
     * Get lead analytics data
     * @return Map containing various lead analytics metrics
     */
    Map<String, Object> getLeadAnalytics();
    
    /**
     * Get lead conversion metrics
     * @return Map containing lead conversion statistics
     */
    Map<String, Object> getLeadConversionMetrics();
    
    /**
     * Get JdbcTemplate for testing purposes
     * @return JdbcTemplate instance
     */
    org.springframework.jdbc.core.JdbcTemplate getJdbcTemplate();
} 