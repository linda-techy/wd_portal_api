package com.wd.api.dto;

import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

/**
 * Base class for search and filter requests across all modules
 * Provides standardized pagination, sorting, and common filter parameters
 * 
 * Enterprise-grade pattern for consistent API design
 * Frontend sends 0-based page index, this class handles it correctly
 */
@Data
public class SearchFilterRequest {
    
    // =====================================================
    // Pagination (0-based from frontend)
    // =====================================================
    private int page = 0;
    private int size = 20;
    
    // =====================================================
    // Sorting
    // =====================================================
    private String sortBy = "id";
    private String sortDirection = "desc"; // "asc" or "desc"
    
    // =====================================================
    // Search
    // =====================================================
    private String search;
    
    // =====================================================
    // Common Filters (used across multiple modules)
    // =====================================================
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    
    /**
     * Convert to Spring Data Pageable object
     * Handles 0-based indexing correctly
     * 
     * @return Pageable object for repository queries
     */
    public Pageable toPageable() {
        // Validate page and size
        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(size, 100)); // Max 100 items per page
        
        // Validate and set default sortBy if null or empty
        String validSortBy = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "id";
        
        // Create sort with null safety
        Sort sort;
        if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
            sort = Sort.by(validSortBy).ascending();
        } else {
            sort = Sort.by(validSortBy).descending();
        }
        
        return PageRequest.of(validPage, validSize, sort);
    }
    
    /**
     * Check if search query is present and not empty
     */
    public boolean hasSearch() {
        return search != null && !search.trim().isEmpty();
    }
    
    /**
     * Get trimmed search query
     */
    public String getSearchQuery() {
        return search != null ? search.trim() : null;
    }
    
    /**
     * Check if status filter is applied
     */
    public boolean hasStatus() {
        return status != null && !status.trim().isEmpty();
    }
    
    /**
     * Check if date range filter is applied
     */
    public boolean hasDateRange() {
        return startDate != null && endDate != null;
    }
}

