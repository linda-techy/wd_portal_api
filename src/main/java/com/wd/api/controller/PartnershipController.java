package com.wd.api.controller;

import com.wd.api.dto.PartnerLoginRequest;
import com.wd.api.dto.PartnerLoginResponse;
import com.wd.api.dto.PartnershipApplicationRequest;
import com.wd.api.dto.PartnershipReferralRequest;
import com.wd.api.dao.model.Leads;
import com.wd.api.service.JwtService;
import com.wd.api.service.PartnershipService;
import com.wd.api.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/partnerships")
@CrossOrigin(origins = "*") // Configure appropriately for production
public class PartnershipController {

    @Autowired
    private PartnershipService partnershipService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LeadService leadService;

    /**
     * Partner Login
     * POST /api/partnerships/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody PartnerLoginRequest request) {
        try {
            PartnerLoginResponse response = partnershipService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Submit Partnership Application
     * POST /api/partnerships/apply
     */
    @PostMapping("/apply")
    public ResponseEntity<?> submitApplication(
            @Valid @RequestBody Map<String, Object> requestBody) {
        try {
            // Extract password from request
            String password = (String) requestBody.get("password");
            if (password == null || password.trim().isEmpty()) {
                throw new RuntimeException("Password is required");
            }

            // Remove password from the map and convert to PartnershipApplicationRequest
            requestBody.remove("password");

            // Map the request (you might want to use a proper mapper)
            PartnershipApplicationRequest request = mapToApplicationRequest(requestBody);

            Map<String, Object> response = partnershipService.submitApplication(request, password);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Partner Logout
     * POST /api/partnerships/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Get Partner Dashboard Stats (Protected Route)
     * GET /api/partnerships/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);

            if (!jwtService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }

            // For now, return mock data
            // TODO: Implement actual stats calculation
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalReferrals", 0);
            stats.put("pendingReferrals", 0);
            stats.put("approvedReferrals", 0);
            stats.put("totalCommission", 0.0);
            stats.put("paidCommission", 0.0);
            stats.put("pendingCommission", 0.0);

            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Get Partner Referrals (Protected Route)
     * GET /api/partnerships/referrals
     */
    @GetMapping("/referrals")
    public ResponseEntity<?> getReferrals(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);

            if (!jwtService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }

            // For now, return empty array
            // TODO: Implement actual referrals retrieval
            return ResponseEntity.ok(new java.util.ArrayList<>());
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Submit New Referral (Protected Route)
     * POST /api/partnerships/referrals
     */
    @PostMapping("/referrals")
    public ResponseEntity<?> submitReferral(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> referralData) {
        try {
            String token = extractToken(authHeader);

            if (!jwtService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }

            // TODO: Implement actual referral submission
            // TODO: Implement actual referral submission
            throw new RuntimeException("Referral submission not implemented yet");
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Submit Referral as Lead (Protected Route)
     * POST /api/partnerships/referrals/lead
     * Creates a lead in the leads table with lead_source = "referral_architect"
     */
    @PostMapping("/referrals/lead")
    public ResponseEntity<?> submitReferralAsLead(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody PartnershipReferralRequest request) {
        try {
            String token = extractToken(authHeader);

            if (!jwtService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }

            // Extract partner information from token
            String phone = jwtService.extractActualSubject(token);
            var partner = partnershipService.getPartnerByPhone(phone);

            if (partner == null) {
                throw new RuntimeException("Partner not found");
            }

            // Set partner information in the request
            request.setPartnerId(partner.getId().toString());
            request.setPartnerName(partner.getFullName());
            request.setPartnershipType(partner.getPartnershipType());

            // Create lead in leads table
            Leads createdLead = leadService.createLeadFromPartnershipReferral(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Referral submitted successfully as lead");
            response.put("leadId", createdLead.getId());
            response.put("leadSource", "referral_architect");
            response.put("clientName", request.getClientName());
            response.put("partnerName", partner.getFullName());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Helper Methods

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid authorization header");
        }
        return authHeader.substring(7);
    }

    private PartnershipApplicationRequest mapToApplicationRequest(Map<String, Object> data) {
        PartnershipApplicationRequest request = new PartnershipApplicationRequest();

        // Primary Contact
        request.setContactName((String) data.get("contactName"));
        request.setContactEmail((String) data.get("contactEmail"));
        request.setContactPhone((String) data.get("contactPhone"));
        request.setDesignation((String) data.get("designation"));

        // Partnership Type
        request.setPartnershipType((String) data.get("partnershipType"));

        // Business Information
        request.setFirmName((String) data.get("firmName"));
        request.setCompanyName((String) data.get("companyName"));
        request.setGstNumber((String) data.get("gstNumber"));
        request.setLicenseNumber((String) data.get("licenseNumber"));
        request.setReraNumber((String) data.get("reraNumber"));
        request.setCinNumber((String) data.get("cinNumber"));
        request.setIfscCode((String) data.get("ifscCode"));
        request.setEmployeeId((String) data.get("employeeId"));

        // Professional Details
        if (data.get("experience") != null) {
            request.setExperience(Integer.valueOf(data.get("experience").toString()));
        }
        request.setSpecialization((String) data.get("specialization"));
        request.setPortfolioLink((String) data.get("portfolioLink"));
        request.setCertifications((String) data.get("certifications"));

        // Operational Details
        request.setAreaOfOperation((String) data.get("areaOfOperation"));
        request.setAreasCovered((String) data.get("areasCovered"));
        request.setLandTypes((String) data.get("landTypes"));
        request.setMaterialsSupplied((String) data.get("materialsSupplied"));
        request.setBusinessSize((String) data.get("businessSize"));
        request.setLocation((String) data.get("location"));
        request.setIndustry((String) data.get("industry"));
        request.setProjectType((String) data.get("projectType"));
        request.setProjectScale((String) data.get("projectScale"));
        request.setTimeline((String) data.get("timeline"));
        if (data.get("yearsOfPractice") != null) {
            request.setYearsOfPractice(Integer.valueOf(data.get("yearsOfPractice").toString()));
        }
        request.setAreaServed((String) data.get("areaServed"));
        request.setBusinessName((String) data.get("businessName"));

        // Additional
        request.setAdditionalContact((String) data.get("additionalContact"));
        request.setMessage((String) data.get("message"));

        return request;
    }
}
