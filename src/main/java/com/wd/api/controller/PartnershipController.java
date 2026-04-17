package com.wd.api.controller;

import com.wd.api.dto.PartnerLoginRequest;
import com.wd.api.dto.PartnerLoginResponse;
import com.wd.api.dto.PartnershipApplicationRequest;
import com.wd.api.dto.PartnershipReferralRequest;
import com.wd.api.model.Lead;
import com.wd.api.service.LeadService;
import com.wd.api.service.PartnershipService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/partnerships")
// CORS configuration is handled globally in SecurityConfig
public class PartnershipController {

    private static final Logger logger = LoggerFactory.getLogger(PartnershipController.class);

    @Autowired
    private PartnershipService partnershipService;

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
            logger.warn("Partner login failed for request: {}", e.getMessage());
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
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password is required"));
            }

            // Remove password from the map and convert to PartnershipApplicationRequest
            requestBody.remove("password");

            // Map the request
            PartnershipApplicationRequest request = mapToApplicationRequest(requestBody);

            Map<String, Object> response = partnershipService.submitApplication(request, password);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            logger.error("Partnership application failed: {}", e.getMessage(), e);
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
     * Forgot Password — send a reset link to the partner's email.
     * POST /api/partnerships/forgot-password
     * Public endpoint — always returns success to prevent email enumeration.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        try {
            partnershipService.sendForgotPasswordEmail(email.trim().toLowerCase());
        } catch (Exception e) {
            logger.error("Error processing partner forgot-password: {}", e.getMessage());
        }
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "If an account with that email exists, a reset link has been sent."
        ));
    }

    /**
     * Reset Password — validate token and set new password.
     * POST /api/partnerships/reset-password
     * Public endpoint.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String token = body.get("token");
        String newPassword = body.get("newPassword");

        if (email == null || token == null || newPassword == null
                || email.isBlank() || token.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email, token, and newPassword are required"));
        }

        try {
            partnershipService.resetPassword(email.trim().toLowerCase(), token.trim(), newPassword);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password reset successfully. You can now log in with your new password."
            ));
        } catch (RuntimeException e) {
            logger.warn("Partner password reset failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get Partner Dashboard Stats (Protected Route)
     * GET /api/partnerships/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> getStats(Authentication auth) {
        var partner = resolvePartner(auth);
        Map<String, Object> stats = partnershipService.getPartnerStats(partner.getId());
        return ResponseEntity.ok(stats);
    }

    /**
     * Get Partner Referrals (Protected Route)
     * GET /api/partnerships/referrals
     */
    @GetMapping("/referrals")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> getReferrals(Authentication auth) {
        var partner = resolvePartner(auth);
        return ResponseEntity.ok(partnershipService.getReferralSummaries(partner.getId()));
    }

    /**
     * Submit New Referral (Protected Route)
     * POST /api/partnerships/referrals
     * Creates a lead with lead_source = "referral_architect"
     */
    @PostMapping("/referrals")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> submitReferral(
            Authentication auth,
            @RequestBody Map<String, Object> referralData) {
        try {
            var partner = resolvePartner(auth);

            // Build a PartnershipReferralRequest from the map
            PartnershipReferralRequest request = new PartnershipReferralRequest();
            request.setClientName((String) referralData.get("clientName"));
            request.setClientEmail((String) referralData.get("clientEmail"));
            request.setClientPhone((String) referralData.get("clientPhone"));
            request.setClientWhatsapp((String) referralData.get("clientWhatsapp"));
            request.setProjectType((String) referralData.get("projectType"));
            request.setProjectDescription((String) referralData.get("projectDescription"));
            request.setLocation((String) referralData.get("location"));
            request.setState((String) referralData.get("state"));
            request.setDistrict((String) referralData.get("district"));
            request.setAddress((String) referralData.get("address"));
            request.setRequirements((String) referralData.get("requirements"));
            request.setNotes((String) referralData.get("notes"));
            request.setPartnerId(partner.getId().toString());
            request.setPartnerName(partner.getFullName());
            request.setPartnershipType(partner.getPartnershipType());

            Lead createdLead = leadService.createLeadFromPartnershipReferral(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Referral submitted successfully");
            response.put("leadId", createdLead.getId());
            response.put("clientName", request.getClientName());
            response.put("partnerName", partner.getFullName());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            logger.error("Failed to submit referral: {}", e.getMessage(), e);
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
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> submitReferralAsLead(
            Authentication auth,
            @Valid @RequestBody PartnershipReferralRequest request) {
        try {
            var partner = resolvePartner(auth);

            // Set partner information in the request
            request.setPartnerId(partner.getId().toString());
            request.setPartnerName(partner.getFullName());
            request.setPartnershipType(partner.getPartnershipType());

            // Create lead in leads table
            Lead createdLead = leadService.createLeadFromPartnershipReferral(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Referral submitted successfully as lead");
            response.put("leadId", createdLead.getId());
            response.put("leadSource", "referral_architect");
            response.put("clientName", request.getClientName());
            response.put("partnerName", partner.getFullName());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            logger.error("Failed to submit referral as lead: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Helper Methods

    /**
     * Resolves the authenticated partner from the Spring Security context.
     * JwtAuthenticationFilter has already validated the token and set the principal;
     * this method simply looks up the partner record and checks their status.
     */
    private com.wd.api.model.PartnershipUser resolvePartner(Authentication auth) {
        String email = auth.getName();
        com.wd.api.model.PartnershipUser partner = partnershipService.getPartnerByEmail(email);
        if (partner == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Partner not found");
        }
        String status = partner.getStatus();
        if (!"active".equalsIgnoreCase(status) && !"approved".equalsIgnoreCase(status)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Partner account is " + status + ". Only active partners can access this resource.");
        }
        return partner;
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
