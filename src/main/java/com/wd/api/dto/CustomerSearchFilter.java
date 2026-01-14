package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Customers module
 * Extends base SearchFilterRequest with customer-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerSearchFilter extends SearchFilterRequest {
    
    // Customer-specific filters
    private String customerType;     // Customer type (INDIVIDUAL, CORPORATE, etc.)
    private String city;             // City
    private String state;            // State
    private String location;         // Location (general)
    private String email;            // Email address
    private String phone;            // Phone number
    private Boolean active;          // Active/Inactive status
    
    /**
     * Check if location filter is applied
     */
    public boolean hasLocation() {
        return (city != null && !city.trim().isEmpty()) ||
               (state != null && !state.trim().isEmpty());
    }
    
    /**
     * Check if customer type filter is applied
     */
    public boolean hasCustomerType() {
        return customerType != null && !customerType.trim().isEmpty();
    }
}

