package com.wd.api.controller;

import com.wd.api.dao.interfaces.ILeadsDAO;
import com.wd.api.dao.model.Leads;
import com.wd.api.dto.LeadCreateRequest;
import com.wd.api.dto.PaginatedResponse;
import com.wd.api.dto.PaginationParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize; // TODO: Re-enable after verifying role names
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/leads")
public class LeadController {
    
    private static final Logger logger = LoggerFactory.getLogger(LeadController.class);

    @Autowired
    private ILeadsDAO leadsDAO;

    // =====================================================
    // LEAD CRUD OPERATIONS
    // =====================================================

    @GetMapping
    // @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // TODO: Re-enable after verifying role names in database
    public ResponseEntity<List<Leads>> getAllLeads() {
        try {
            List<Leads> leads = leadsDAO.getAllLeads();
            return ResponseEntity.ok(leads);
        } catch (Exception e) {
            logger.error("Error in getAllLeads controller", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/paginated")
    // @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // TODO: Re-enable after verifying role names in database
    public ResponseEntity<PaginatedResponse<Leads>> getLeadsPaginated(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String customerType,
            @RequestParam(required = false) String projectType,
            @RequestParam(required = false) String assignedTeam,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Double minBudget,
            @RequestParam(required = false) Double maxBudget,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "created_at") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            // Input validation
            if (page < 1) {
                return ResponseEntity.badRequest().body(null);
            }
            if (limit < 1 || limit > 100) {
                return ResponseEntity.badRequest().body(null);
            }
            if (minBudget != null && minBudget < 0) {
                return ResponseEntity.badRequest().body(null);
            }
            if (maxBudget != null && maxBudget < 0) {
                return ResponseEntity.badRequest().body(null);
            }
            if (minBudget != null && maxBudget != null && minBudget > maxBudget) {
                return ResponseEntity.badRequest().body(null);
            }
            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest().body(null);
            }
            if (!isValidSortBy(sortBy)) {
                return ResponseEntity.badRequest().body(null);
            }
            if (!isValidSortOrder(sortOrder)) {
                return ResponseEntity.badRequest().body(null);
            }

            PaginationParams params = new PaginationParams();
            params.setPage(page);
            params.setLimit(limit);
            params.setSearch(search);
            params.setStatus(status);
            params.setSource(source);
            params.setPriority(priority);
            params.setCustomerType(customerType);
            params.setProjectType(projectType);
            params.setAssignedTeam(assignedTeam);
            params.setState(state);
            params.setDistrict(district);
            params.setMinBudget(minBudget);
            params.setMaxBudget(maxBudget);
            params.setStartDate(startDate);
            params.setEndDate(endDate);
            params.setSortBy(sortBy);
            params.setSortOrder(sortOrder);

            PaginatedResponse<Leads> response = leadsDAO.getLeadsPaginated(params);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in getLeadsPaginated controller", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    private boolean isValidSortBy(String sortBy) {
        return sortBy != null && (sortBy.equals("created_at") || sortBy.equals("name") || 
                sortBy.equals("email") || sortBy.equals("phone") || sortBy.equals("status") || 
                sortBy.equals("priority") || sortBy.equals("budget") || sortBy.equals("updated_at"));
    }

    private boolean isValidSortOrder(String sortOrder) {
        return sortOrder != null && (sortOrder.equalsIgnoreCase("asc") || sortOrder.equalsIgnoreCase("desc"));
    }

    @GetMapping("/{leadId}")
    public ResponseEntity<Leads> getLeadById(@PathVariable String leadId) {
        try {
            Leads lead = leadsDAO.getLeadById(leadId);
            if (lead != null) {
                return ResponseEntity.ok(lead);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createLead(@RequestBody LeadCreateRequest request) {
        try {
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "name is required"));
            }
            // Convert DTO to Leads model
            Leads lead = new Leads();
            // lead_id will be auto-generated by database (bigserial)
            lead.setName(request.getName());
            String email = request.getEmail();
            if (email == null || email.trim().isEmpty()) {
                email = ""; // Set empty string instead of null for required field
            }
            lead.setEmail(email);
            
            String phone = request.getPhone();
            if (phone == null || phone.trim().isEmpty()) {
                phone = ""; // Set empty string instead of null for required field
            }
            lead.setPhone(phone);
            
            String whatsapp = request.getWhatsappNumber();
            if (whatsapp == null || whatsapp.trim().isEmpty()) {
                whatsapp = ""; // Set empty string instead of null for required field
            }
            lead.setWhatsappNumber(whatsapp);
            String customerType = request.getCustomerType();
            if (customerType != null && !customerType.trim().isEmpty()) {
                customerType = customerType.trim().toLowerCase(); // Normalize to lowercase
                // Validate against allowed values
                if (!isValidCustomerType(customerType)) {
                    customerType = "other"; // Default to 'other' if invalid
                }
            } else {
                customerType = "other"; // Set default value for empty/null
            }
            lead.setCustomerType(customerType);
            
            // Handle project-related fields
            String projectType = request.getProjectType();
            if (projectType == null || projectType.trim().isEmpty()) {
                projectType = ""; // Default for required field
            }
            lead.setProjectType(projectType);
            
            String projectDescription = request.getProjectDescription();
            if (projectDescription == null || projectDescription.trim().isEmpty()) {
                projectDescription = ""; // Default for required field
            }
            lead.setProjectDescription(projectDescription);
            
            String requirements = request.getRequirements();
            if (requirements == null || requirements.trim().isEmpty()) {
                requirements = ""; // Default for required field
            }
            lead.setRequirements(requirements);
            
            lead.setBudget(request.getBudget());
            lead.setProjectSqftArea(request.getProjectSqftArea());
            String status = request.getLeadStatus();
            if (status == null || status.trim().isEmpty()) {
                status = "new_inquiry";
            } else {
                status = status.trim().toLowerCase().replace(' ', '_');
                if (!isValidLeadStatus(status)) {
                    status = "new_inquiry"; // Default to valid status
                }
            }
            lead.setLeadStatus(status);

            String source = request.getLeadSource();
            if (source == null || source.trim().isEmpty()) {
                source = "website";
            } else {
                source = source.trim().toLowerCase().replace(' ', '_');
                if (!isValidLeadSource(source)) {
                    source = "website"; // Default to valid source
                }
            }
            lead.setLeadSource(source);
            
            String priority = request.getPriority();
            if (priority == null || priority.trim().isEmpty()) {
                priority = "low";
            } else {
                priority = priority.trim().toLowerCase();
                if (!isValidPriority(priority)) {
                    priority = "low"; // Default to valid priority
                }
            }
            lead.setPriority(priority);
            
            // Handle assigned team field
            String assignedTeam = request.getAssignedTeam();
            if (assignedTeam == null || assignedTeam.trim().isEmpty()) {
                assignedTeam = ""; // Default for required field
            }
            lead.setAssignedTeam(assignedTeam);
            
            // Handle notes field
            String notes = request.getNotes();
            if (notes == null || notes.trim().isEmpty()) {
                notes = ""; // Default for required field
            }
            lead.setNotes(notes);
            
            
            // Handle lost reason
            String lostReason = request.getLostReason();
            if (lostReason == null || lostReason.trim().isEmpty()) {
                lostReason = null; // Can be null
            }
            lead.setLostReason(lostReason);
            
            // Handle client rating and probability to win
            Integer clientRating = request.getClientRating();
            if (clientRating == null) {
                clientRating = 0; // Default value
            }
            lead.setClientRating(clientRating);
            
            Integer probabilityToWin = request.getProbabilityToWin();
            if (probabilityToWin == null) {
                probabilityToWin = 0; // Default value
            }
            lead.setProbabilityToWin(probabilityToWin);
            
            lead.setNextFollowUp(request.getNextFollowUp());
            lead.setLastContactDate(request.getLastContactDate());
            
            // Set location fields - allow null values as per DB schema
            lead.setState(request.getState());
            lead.setDistrict(request.getDistrict());
            lead.setLocation(request.getLocation());
            lead.setAddress(request.getAddress());
            
            // Set date of enquiry - parse from ISO string if provided
            if (request.getDateOfEnquiry() != null && !request.getDateOfEnquiry().trim().isEmpty()) {
                try {
                    // Parse ISO string to LocalDate (extract date part only)
                    LocalDate parsedDate = LocalDate.parse(request.getDateOfEnquiry().substring(0, 10));
                    lead.setDateOfEnquiry(parsedDate);
                } catch (Exception e) {
                    // If parsing fails, use current date
                    lead.setDateOfEnquiry(LocalDate.now());
                }
            } else {
                lead.setDateOfEnquiry(LocalDate.now());
            }
            
            int result = leadsDAO.createLead(lead);
            if (result > 0) {
                // Get the created lead using the auto-generated lead_id
                // We need to get the last inserted lead or use a different approach
                // For now, return the lead object we created
                return ResponseEntity.ok(lead);
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            logger.error("Error creating lead", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{leadId}")
    public ResponseEntity<Leads> updateLead(@PathVariable String leadId, @RequestBody Leads lead) {
        try {
            logger.debug("Updating lead with ID: {}, request data: {}", leadId, lead);
            
            // Set the lead ID from path parameter (override any ID in request body)
            lead.setLeadId(leadId);
            
            // Validate required fields
            if (lead.getName() == null || lead.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Normalize and validate customer type
            if (lead.getCustomerType() != null && !lead.getCustomerType().trim().isEmpty()) {
                String customerType = lead.getCustomerType().trim().toLowerCase();
                if (!isValidCustomerType(customerType)) {
                    customerType = "other"; // Default to 'other' if invalid
                }
                lead.setCustomerType(customerType);
            } else {
                lead.setCustomerType("other"); // Set default value for empty/null
            }
            
            // Normalize and validate lead source
            if (lead.getLeadSource() != null && !lead.getLeadSource().trim().isEmpty()) {
                String leadSource = lead.getLeadSource().trim().toLowerCase();
                if (!isValidLeadSource(leadSource)) {
                    leadSource = "website"; // Default to 'website' if invalid
                }
                lead.setLeadSource(leadSource);
            } else {
                lead.setLeadSource("website"); // Set default value for empty/null
            }
            
            // Normalize and validate lead status
            if (lead.getLeadStatus() != null && !lead.getLeadStatus().trim().isEmpty()) {
                String leadStatus = lead.getLeadStatus().trim().toLowerCase();
                if (!isValidLeadStatus(leadStatus)) {
                    leadStatus = "new_inquiry"; // Default to 'new_inquiry' if invalid
                }
                lead.setLeadStatus(leadStatus);
            } else {
                lead.setLeadStatus("new_inquiry"); // Set default value for empty/null
            }
            
            // Normalize and validate priority
            if (lead.getPriority() != null && !lead.getPriority().trim().isEmpty()) {
                String priority = lead.getPriority().trim().toLowerCase();
                if (!isValidPriority(priority)) {
                    priority = "low"; // Default to 'low' if invalid
                }
                lead.setPriority(priority);
            } else {
                lead.setPriority("low"); // Set default value for empty/null
            }
            
            int result = leadsDAO.updateLead(lead);
            if (result > 0) {
                return ResponseEntity.ok(lead);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error updating lead with ID: {}", leadId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLead(@PathVariable String id) {
        try {
            int result = leadsDAO.deleteLead(id);
            if (result > 0) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // =====================================================
    // LEAD FILTERING & SEARCH
    // =====================================================

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Leads>> getLeadsByStatus(@PathVariable String status) {
        try {
            List<Leads> leads = leadsDAO.getLeadsByStatus(status);
            return ResponseEntity.ok(leads);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/assigned/{teamMemberId}")
    public ResponseEntity<List<Leads>> getLeadsByAssignedTo(@PathVariable UUID teamMemberId) {
        try {
            List<Leads> leads = leadsDAO.getLeadsByAssignedTo(teamMemberId);
            return ResponseEntity.ok(leads);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/source/{source}")
    public ResponseEntity<List<Leads>> getLeadsBySource(@PathVariable String source) {
        try {
            List<Leads> leads = leadsDAO.getLeadsBySource(source);
            return ResponseEntity.ok(leads);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<Leads>> getLeadsByPriority(@PathVariable String priority) {
        try {
            List<Leads> leads = leadsDAO.getLeadsByPriority(priority);
            return ResponseEntity.ok(leads);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<Leads>> getLeadsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<Leads> leads = leadsDAO.getLeadsByDateRange(startDate, endDate);
            return ResponseEntity.ok(leads);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Leads>> searchLeads(@RequestParam String query) {
        try {
            List<Leads> leads = leadsDAO.searchLeads(query);
            return ResponseEntity.ok(leads);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // =====================================================
    // LEAD MANAGEMENT OPERATIONS
    // =====================================================

    @PutMapping("/{id}/assign")
    public ResponseEntity<Void> assignLeadToTeamMember(
            @PathVariable String id, 
            @RequestParam UUID teamMemberId) {
        try {
            int result = leadsDAO.assignLeadToTeamMember(id, teamMemberId);
            if (result > 0) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateLeadStatus(
            @PathVariable String id, 
            @RequestParam String status) {
        try {
            int result = leadsDAO.updateLeadStatus(id, status);
            if (result > 0) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}/priority")
    public ResponseEntity<Void> updateLeadPriority(
            @PathVariable String id, 
            @RequestParam String priority) {
        try {
            int result = leadsDAO.updateLeadPriority(id, priority);
            if (result > 0) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}/convert")
    public ResponseEntity<Void> markLeadAsConverted(@PathVariable String id) {
        try {
            int result = leadsDAO.markLeadAsConverted(id);
            if (result > 0) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}/lost")
    public ResponseEntity<Void> markLeadAsLost(
            @PathVariable String id, 
            @RequestParam String reason) {
        try {
            int result = leadsDAO.markLeadAsLost(id, reason);
            if (result > 0) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // =====================================================
    // ANALYTICS & REPORTS
    // =====================================================

    @GetMapping("/overdue-followups")
    public ResponseEntity<List<Leads>> getOverdueFollowUps() {
        try {
            List<Leads> leads = leadsDAO.getOverdueFollowUps();
            return ResponseEntity.ok(leads);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getLeadAnalytics() {
        try {
            Map<String, Object> analytics = leadsDAO.getLeadAnalytics();
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/conversion-metrics")
    public ResponseEntity<Map<String, Object>> getLeadConversionMetrics() {
        try {
            Map<String, Object> metrics = leadsDAO.getLeadConversionMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testLeadsConnection() {
        try {
            // Test if we can connect to the database and if the leads table exists
            String sql = "SELECT COUNT(*) FROM leads";
            Integer count = leadsDAO.getJdbcTemplate().queryForObject(sql, Integer.class);
            return ResponseEntity.ok("Database connection successful. Leads count: " + count);
        } catch (Exception e) {
            return ResponseEntity.ok("Database error: " + e.getMessage());
        }
    }

    @GetMapping("/schema")
    public ResponseEntity<String> getLeadsSchema() {
        try {
            // Get table schema information
            String sql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'leads' ORDER BY ordinal_position";
            List<Map<String, Object>> columns = leadsDAO.getJdbcTemplate().queryForList(sql);
            StringBuilder schema = new StringBuilder("Leads table schema:\n");
            for (Map<String, Object> column : columns) {
                schema.append(column.get("column_name")).append(": ").append(column.get("data_type")).append("\n");
            }
            return ResponseEntity.ok(schema.toString());
        } catch (Exception e) {
            return ResponseEntity.ok("Schema error: " + e.getMessage());
        }
    }
    
    /**
     * Debug endpoint - REMOVE IN PRODUCTION
     * This endpoint should be secured with admin-only access or removed entirely
     * TODO: Remove this endpoint before production deployment
     */
    // @GetMapping("/debug/{leadId}")
    // @PreAuthorize("hasRole('ADMIN')") // Uncomment and secure if needed
    // public ResponseEntity<String> debugLead(@PathVariable String leadId) {
    //     try {
    //         Leads lead = leadsDAO.getLeadById(leadId);
    //         if (lead != null) {
    //             return ResponseEntity.ok("Lead found: " + lead.toString());
    //         } else {
    //             return ResponseEntity.ok("Lead not found with ID: " + leadId);
    //         }
    //     } catch (Exception e) {
    //         return ResponseEntity.ok("Debug error: " + e.getMessage());
    //     }
    // }
    
    /**
     * Validates if the customer type is allowed by the database constraint
     */
    private boolean isValidCustomerType(String customerType) {
        return customerType != null && (
            customerType.equals("individual") ||
            customerType.equals("business") ||
            customerType.equals("government") ||
            customerType.equals("institution") ||
            customerType.equals("channel_partner") ||
            customerType.equals("other")
        );
    }
    
    /**
     * Validates if the lead source is allowed by the database constraint
     */
    private boolean isValidLeadSource(String leadSource) {
        return leadSource != null && (
            leadSource.equals("website") ||
            leadSource.equals("whatsapp") ||
            leadSource.equals("calculator") ||
            leadSource.equals("google_business_profile") ||
            leadSource.equals("referral_client") ||
            leadSource.equals("referral_architect") ||
            leadSource.equals("social_media") ||
            leadSource.equals("whatsapp_business") ||
            leadSource.equals("online_ads") ||
            leadSource.equals("direct_walkin") ||
            leadSource.equals("event_trade_show") ||
            leadSource.equals("print_advertising")
        );
    }
    
    /**
     * Validates if the lead status is allowed by the database constraint
     */
    private boolean isValidLeadStatus(String leadStatus) {
        return leadStatus != null && (
            leadStatus.equals("new_inquiry") ||
            leadStatus.equals("contacted") ||
            leadStatus.equals("qualified_lead") ||
            leadStatus.equals("proposal_sent") ||
            leadStatus.equals("negotiation") ||
            leadStatus.equals("project_won") ||
            leadStatus.equals("lost")
        );
    }
    
    /**
     * Validates if the priority is allowed by the database constraint
     */
    private boolean isValidPriority(String priority) {
        return priority != null && (
            priority.equals("low") ||
            priority.equals("medium") ||
            priority.equals("high")
        );
    }
    
    // =====================================================
    // PUBLIC API ENDPOINTS (No Authentication Required)
    // =====================================================
    
    /**
     * Public endpoint for contact form submissions
     * POST /leads/contact
     */
    @PostMapping("/contact")
    public ResponseEntity<?> submitContactFormLead(@RequestBody Map<String, Object> contactData) {
        try {
            Leads lead = new Leads();
            
            // Client Information
            lead.setName((String) contactData.get("name"));
            lead.setEmail((String) contactData.get("email"));
            lead.setPhone((String) contactData.get("phone"));
            lead.setWhatsappNumber((String) contactData.get("phone")); // Use phone as WhatsApp
            
            // Lead Source & Status
            lead.setLeadSource("website");
            lead.setLeadStatus("new_inquiry");
            
            // Customer Type & Priority (defaults)
            lead.setCustomerType("individual");
            lead.setPriority("medium");
            
            // Project Information
            String projectType = (String) contactData.get("projectType");
            lead.setProjectType(projectType != null && !projectType.isEmpty() ? projectType : "turnkey_project");
            
            String message = (String) contactData.get("message");
            lead.setProjectDescription(message != null ? message : "");
            lead.setRequirements("");
            
            // Location Information
            lead.setState((String) contactData.get("state"));
            lead.setDistrict((String) contactData.get("district"));
            lead.setLocation("");
            lead.setAddress("");
            
            // Tracking Fields (defaults)
            lead.setBudget(null);
            lead.setProjectSqftArea(null);
            lead.setAssignedTeam("");
            lead.setClientRating(3);
            lead.setProbabilityToWin(50);
            lead.setNextFollowUp(null);
            lead.setLastContactDate(null);
            lead.setLostReason(null);
            lead.setDateOfEnquiry(LocalDate.now());
            
            // Notes
            lead.setNotes("Contact Form Submission: " + (message != null ? message : ""));
            
            // Create lead
            int result = leadsDAO.createLead(lead);
            if (result > 0) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thank you! We'll contact you soon.",
                    "leadSource", "website"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to submit inquiry"
                ));
            }
        } catch (Exception e) {
            logger.error("Error creating contact form lead", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "An error occurred. Please try again."
            ));
        }
    }
    
    /**
     * Public endpoint for client referral submissions
     * POST /leads/referral
     */
    @PostMapping("/referral")
    public ResponseEntity<?> submitClientReferralLead(@RequestBody Map<String, Object> referralData) {
        try {
            Leads lead = new Leads();
            
            // Referral Information (the person being referred)
            lead.setName((String) referralData.get("referralName"));
            lead.setEmail((String) referralData.get("referralEmail"));
            lead.setPhone((String) referralData.get("referralPhone"));
            lead.setWhatsappNumber((String) referralData.get("referralPhone"));
            
            // Lead Source & Status
            lead.setLeadSource("referral_client");
            lead.setLeadStatus("new_inquiry");
            
            // Customer Type & Priority (defaults)
            lead.setCustomerType("individual");
            lead.setPriority("medium");
            
            // Project Information
            String projectType = (String) referralData.get("projectType");
            lead.setProjectType(projectType != null && !projectType.isEmpty() ? projectType : "turnkey_project");
            
            // Parse budget if provided
            String budgetStr = (String) referralData.get("estimatedBudget");
            java.math.BigDecimal budget = null;
            if (budgetStr != null && !budgetStr.isEmpty()) {
                // Convert budget range text to numeric value
                if (budgetStr.contains("15-25")) budget = new java.math.BigDecimal("2000000");
                else if (budgetStr.contains("25-50")) budget = new java.math.BigDecimal("3750000");
                else if (budgetStr.contains("50-75")) budget = new java.math.BigDecimal("6250000");
                else if (budgetStr.contains("75") || budgetStr.contains("1 Crore")) budget = new java.math.BigDecimal("8750000");
                else if (budgetStr.contains("Above") || budgetStr.contains("1 Crore")) budget = new java.math.BigDecimal("15000000");
            }
            lead.setBudget(budget);
            
            String location = (String) referralData.get("location");
            String message = (String) referralData.get("message");
            
            lead.setProjectDescription(message != null ? message : "");
            lead.setRequirements("");
            
            // Location Information (parse from location field)
            lead.setState("Kerala"); // Default state for referrals
            lead.setDistrict("");
            lead.setLocation(location != null ? location : "");
            lead.setAddress("");
            
            // Tracking Fields (defaults)
            lead.setProjectSqftArea(null);
            lead.setAssignedTeam("");
            lead.setClientRating(3);
            lead.setProbabilityToWin(50);
            lead.setNextFollowUp(null);
            lead.setLastContactDate(null);
            lead.setLostReason(null);
            lead.setDateOfEnquiry(LocalDate.now());
            
            // Notes - Include referrer information
            String yourName = (String) referralData.get("yourName");
            String yourEmail = (String) referralData.get("yourEmail");
            String yourPhone = (String) referralData.get("yourPhone");
            
            String notes = String.format(
                "Client Referral\nReferred by: %s (Email: %s, Phone: %s)",
                yourName != null ? yourName : "Unknown",
                yourEmail != null ? yourEmail : "N/A",
                yourPhone != null ? yourPhone : "N/A"
            );
            
            if (message != null && !message.trim().isEmpty()) {
                notes += "\n\nAdditional Message: " + message;
            }
            
            lead.setNotes(notes);
            
            // Create lead
            int result = leadsDAO.createLead(lead);
            if (result > 0) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thank you! Your referral has been submitted successfully.",
                    "leadSource", "referral_client"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to submit referral"
                ));
            }
        } catch (Exception e) {
            logger.error("Error creating client referral lead", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "An error occurred. Please try again."
            ));
        }
    }
    
    /**
     * Public endpoint for home cost calculator submissions
     * POST /leads/calculator/home-cost
     */
    @PostMapping("/calculator/home-cost")
    public ResponseEntity<?> submitCalculatorLead(@RequestBody Map<String, Object> calculatorData) {
        try {
            Leads lead = new Leads();
            
            // Client Information (WhatsApp number only from calculator)
            String whatsappNumber = (String) calculatorData.get("whatsappNumber");
            
            // Generate random short UUID for name (e.g., "CALC-a3b5c7d9")
            String shortUUID = "CALC-" + UUID.randomUUID().toString().substring(0, 8);
            lead.setName(shortUUID);
            
            lead.setEmail(""); // Not captured in calculator
            
            // Set WhatsApp number in both phone and whatsapp_number columns
            String phoneNumber = whatsappNumber != null ? whatsappNumber : "";
            lead.setPhone(phoneNumber);
            lead.setWhatsappNumber(phoneNumber);
            
            // Lead Source & Status
            lead.setLeadSource("calculator_home_cost");
            lead.setLeadStatus("new_inquiry");
            
            // Customer Type & Priority (defaults)
            lead.setCustomerType("individual");
            lead.setPriority("medium");
            
            // Project Information from calculator
            String typeOfConstruction = (String) calculatorData.get("typeOfConstruction");
            String projectType = "turnkey_project"; // Default
            
            // Map construction type to project type
            if (typeOfConstruction != null) {
                switch (typeOfConstruction.toLowerCase()) {
                    case "basic":
                        projectType = "residential_construction";
                        break;
                    case "premium":
                        projectType = "turnkey_project";
                        break;
                    case "luxury":
                        projectType = "turnkey_project";
                        break;
                    default:
                        projectType = "turnkey_project";
                }
            }
            lead.setProjectType(projectType);
            
            // Parse costs from calculator
            Object totalCostMinObj = calculatorData.get("totalCostMin");
            Object totalCostMaxObj = calculatorData.get("totalCostMax");
            
            java.math.BigDecimal budget = null;
            if (totalCostMinObj != null && totalCostMaxObj != null) {
                // Take average of min and max
                double minCost = totalCostMinObj instanceof Number ? ((Number) totalCostMinObj).doubleValue() : 0;
                double maxCost = totalCostMaxObj instanceof Number ? ((Number) totalCostMaxObj).doubleValue() : 0;
                double avgCost = (minCost + maxCost) / 2;
                budget = new java.math.BigDecimal(avgCost);
            }
            lead.setBudget(budget);
            
            // Parse construction area
            Object constructionAreaObj = calculatorData.get("totalConstructionArea");
            java.math.BigDecimal projectSqftArea = null;
            if (constructionAreaObj != null) {
                double area = constructionAreaObj instanceof Number ? ((Number) constructionAreaObj).doubleValue() : 0;
                
                // Convert to sqft if in sqm
                String unitOfArea = (String) calculatorData.get("unitOfArea");
                if ("sqm".equalsIgnoreCase(unitOfArea)) {
                    area = area * 10.7639; // Convert sqm to sqft
                }
                
                projectSqftArea = new java.math.BigDecimal(area);
            }
            lead.setProjectSqftArea(projectSqftArea);
            
            // Project description from calculator data
            String description = String.format(
                "Home Cost Calculator Estimate\nType: %s\nArea: %.2f %s\nEstimated Cost: ₹%.2f - ₹%.2f",
                typeOfConstruction != null ? typeOfConstruction : "N/A",
                constructionAreaObj instanceof Number ? ((Number) constructionAreaObj).doubleValue() : 0,
                calculatorData.get("unitOfArea") != null ? calculatorData.get("unitOfArea") : "sqft",
                totalCostMinObj instanceof Number ? ((Number) totalCostMinObj).doubleValue() : 0,
                totalCostMaxObj instanceof Number ? ((Number) totalCostMaxObj).doubleValue() : 0
            );
            lead.setProjectDescription(description);
            lead.setRequirements("");
            
            // Location Information
            String state = (String) calculatorData.get("state");
            String district = (String) calculatorData.get("district");
            
            // Capitalize first letter to match portal format (e.g., "thrissur" -> "Thrissur")
            if (state != null && !state.isEmpty()) {
                state = state.substring(0, 1).toUpperCase() + state.substring(1);
            }
            if (district != null && !district.isEmpty()) {
                district = district.substring(0, 1).toUpperCase() + district.substring(1);
            }
            
            lead.setState(state);
            lead.setDistrict(district);
            lead.setLocation("");
            lead.setAddress("");
            
            // Tracking Fields (defaults)
            lead.setAssignedTeam("");
            lead.setClientRating(3);
            lead.setProbabilityToWin(50);
            lead.setNextFollowUp(null);
            lead.setLastContactDate(null);
            lead.setLostReason(null);
            lead.setDateOfEnquiry(LocalDate.now());
            
            // Notes
            lead.setNotes("Submitted via Home Cost Calculator");
            
            // Create lead
            int result = leadsDAO.createLead(lead);
            if (result > 0) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Estimate saved successfully!",
                    "leadSource", "calculator_home_cost"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to save estimate"
                ));
            }
        } catch (Exception e) {
            logger.error("Error creating calculator lead", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "An error occurred. Please try again."
            ));
        }
    }
} 