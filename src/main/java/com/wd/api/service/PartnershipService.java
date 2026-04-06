package com.wd.api.service;

import com.wd.api.dto.PartnerLoginRequest;
import com.wd.api.dto.PartnerLoginResponse;
import com.wd.api.dto.PartnershipApplicationRequest;
import com.wd.api.model.CustomerPasswordResetToken;
import com.wd.api.model.Lead;
import com.wd.api.model.PartnershipUser;
import com.wd.api.repository.CustomerPasswordResetTokenRepository;
import com.wd.api.repository.LeadRepository;
import com.wd.api.repository.PartnershipUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PartnershipService {

    private static final Logger logger = LoggerFactory.getLogger(PartnershipService.class);

    @Autowired
    private PartnershipUserRepository partnershipUserRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired
    private CustomerPasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.website-base-url:https://walldotbuilders.com}")
    private String websiteBaseUrl;

    /**
     * Partner Login
     */
    public PartnerLoginResponse login(PartnerLoginRequest request) {
        // Find user by email
        PartnershipUser partner = partnershipUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), partner.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Check if account is active
        if (!"active".equals(partner.getStatus()) && !"approved".equals(partner.getStatus())) {
            throw new RuntimeException("Account is not active. Status: " + partner.getStatus());
        }

        // Update last login and status to active
        partner.setLastLogin(LocalDateTime.now());
        if ("approved".equals(partner.getStatus())) {
            partner.setStatus("active");
        }
        partnershipUserRepository.save(partner);

        // Generate JWT token with PARTNER prefix
        Map<String, Object> claims = new HashMap<>();
        claims.put("partnerId", partner.getId().toString());
        claims.put("partnershipType", partner.getPartnershipType());
        claims.put("status", partner.getStatus());

        String token = jwtService.generatePartnerToken(partner.getEmail(), claims);

        // Create response
        return new PartnerLoginResponse(
                token,
                partner.getId().toString(),
                partner.getFullName(),
                partner.getPhone(),
                partner.getEmail(),
                partner.getPartnershipType(),
                partner.getFirmName() != null ? partner.getFirmName() : partner.getCompanyName(),
                partner.getStatus());
    }

    /**
     * Submit Partnership Application
     */
    @Transactional
    public Map<String, Object> submitApplication(PartnershipApplicationRequest request, String password) {
        // Check if phone or email already exists
        if (partnershipUserRepository.existsByPhone(request.getContactPhone())) {
            throw new RuntimeException("Phone number already registered");
        }
        if (partnershipUserRepository.existsByEmail(request.getContactEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Create new partnership user
        PartnershipUser partner = new PartnershipUser();

        // Primary contact info
        partner.setFullName(request.getContactName());
        partner.setEmail(request.getContactEmail());
        partner.setPhone(request.getContactPhone());
        partner.setDesignation(request.getDesignation());

        // Hash password
        partner.setPasswordHash(passwordEncoder.encode(password));

        // Partnership details
        partner.setPartnershipType(request.getPartnershipType());

        // Business information
        partner.setFirmName(request.getFirmName());
        partner.setCompanyName(request.getCompanyName());
        partner.setGstNumber(request.getGstNumber());
        partner.setLicenseNumber(request.getLicenseNumber());
        partner.setReraNumber(request.getReraNumber());
        partner.setCinNumber(request.getCinNumber());
        partner.setIfscCode(request.getIfscCode());
        partner.setEmployeeId(request.getEmployeeId());

        // Professional details
        partner.setExperience(request.getExperience());
        partner.setSpecialization(request.getSpecialization());
        partner.setPortfolioLink(request.getPortfolioLink());
        partner.setCertifications(request.getCertifications());

        // Operational details
        partner.setAreaOfOperation(request.getAreaOfOperation());
        partner.setAreasCovered(request.getAreasCovered());
        partner.setLandTypes(request.getLandTypes());
        partner.setMaterialsSupplied(request.getMaterialsSupplied());
        partner.setBusinessSize(request.getBusinessSize());
        partner.setLocation(request.getLocation());
        partner.setIndustry(request.getIndustry());
        partner.setProjectType(request.getProjectType());
        partner.setProjectScale(request.getProjectScale());
        partner.setTimeline(request.getTimeline());
        partner.setYearsOfPractice(request.getYearsOfPractice());
        partner.setAreaServed(request.getAreaServed());
        partner.setBusinessName(request.getBusinessName());

        // Additional info
        partner.setAdditionalContact(request.getAdditionalContact());
        partner.setMessage(request.getMessage());

        // Set status as pending (requires admin approval)
        partner.setStatus("pending");

        // Save
        PartnershipUser savedPartner = partnershipUserRepository.save(partner);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Partnership application submitted successfully");
        response.put("partnerId", savedPartner.getId());
        response.put("status", "pending");
        response.put("note", "Your application is under review. You will be able to login once approved.");

        return response;
    }

    /**
     * Get Partner by ID
     */
    public PartnershipUser getPartnerById(Long partnerId) {
        return partnershipUserRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found"));
    }

    /**
     * Get partner by email
     */
    public PartnershipUser getPartnerByEmail(String email) {
        return partnershipUserRepository.findByEmail(email).orElse(null);
    }

    /**
     * Update Partner Status (Admin function)
     */
    @Transactional
    public void updatePartnerStatus(Long partnerId, String status, String updatedBy) {
        PartnershipUser partner = getPartnerById(partnerId);
        partner.setStatus(status);
        partner.setUpdatedBy(updatedBy);

        if ("approved".equals(status)) {
            partner.setApprovedAt(LocalDateTime.now());
        }

        partnershipUserRepository.save(partner);

        // Send notification emails asynchronously
        if ("approved".equals(status)) {
            emailService.sendPartnerApprovalEmail(
                    partner.getEmail(),
                    partner.getFullName(),
                    partner.getPartnershipType() != null ? partner.getPartnershipType() : "Partner");
        } else if ("rejected".equals(status)) {
            emailService.sendPartnerRejectionEmail(partner.getEmail(), partner.getFullName());
        }
    }

    /**
     * Get referral leads submitted by a specific partner.
     * - Professional partners (architects, etc.): leadSource=referral_architect, notes contain "(ID: {partnerId})"
     * - Individual referrers with tracking accounts: leadSource=referral_client, notes contain "Partner ID: {partnerId}"
     */
    public List<Lead> getReferralsByPartner(Long partnerId) {
        PartnershipUser partner = partnershipUserRepository.findById(partnerId).orElse(null);
        if (partner != null && "referral_client".equals(partner.getPartnershipType())) {
            // Individual referrer tracking account
            String fragment = "Partner ID: " + partnerId;
            return leadRepository.findByLeadSourceAndNotesContaining("referral_client", fragment);
        }
        // Professional partner (architect, designer, etc.)
        String partnerIdFragment = "(ID: " + partnerId + ")";
        return leadRepository.findByLeadSourceAndNotesContaining("referral_architect", partnerIdFragment);
    }

    /**
     * Get partnership dashboard statistics for a specific partner.
     */
    public Map<String, Object> getPartnerStats(Long partnerId) {
        List<Lead> referrals = getReferralsByPartner(partnerId);

        long totalReferrals = referrals.size();
        long pendingReferrals = referrals.stream()
                .filter(l -> {
                    String status = normalizeLeadStatus(l.getLeadStatus());
                    return "new_inquiry".equals(status) || "contacted".equals(status);
                })
                .count();
        long qualifiedReferrals = referrals.stream()
                .filter(l -> {
                    String status = normalizeLeadStatus(l.getLeadStatus());
                    return "qualified".equals(status) || "proposal_sent".equals(status) || "negotiation".equals(status);
                })
                .count();
        long convertedReferrals = referrals.stream()
                .filter(l -> "project_won".equals(normalizeLeadStatus(l.getLeadStatus())))
                .count();
        long lostReferrals = referrals.stream()
                .filter(l -> "lost".equals(normalizeLeadStatus(l.getLeadStatus())))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReferrals", totalReferrals);
        stats.put("pendingReferrals", pendingReferrals);
        stats.put("qualifiedReferrals", qualifiedReferrals);
        stats.put("convertedReferrals", convertedReferrals);
        stats.put("lostReferrals", lostReferrals);

        return stats;
    }

    /**
     * Get referrals as a list of summary maps for the partner dashboard.
     */
    public List<Map<String, Object>> getReferralSummaries(Long partnerId) {
        List<Lead> referrals = getReferralsByPartner(partnerId);
        List<Map<String, Object>> summaries = new ArrayList<>();

        for (Lead lead : referrals) {
            Map<String, Object> summary = new HashMap<>();
            summary.put("leadId", lead.getId());
            summary.put("clientName", lead.getName());
            summary.put("clientPhone", lead.getPhone());
            summary.put("clientEmail", lead.getEmail());
            summary.put("projectType", lead.getProjectType());
            summary.put("status", normalizeLeadStatus(lead.getLeadStatus()));
            summary.put("priority", lead.getPriority());
            summary.put("location", lead.getLocation());
            summary.put("budget", lead.getBudget());
            summary.put("dateOfEnquiry", lead.getDateOfEnquiry());
            summary.put("createdAt", lead.getCreatedAt());
            summaries.add(summary);
        }

        return summaries;
    }

    // ── Password reset ──────────────────────────────────────────────────────

    /**
     * Generates a reset token and sends a reset-password email to the partner.
     * Uses the shared customer_password_reset_tokens table (keyed by email).
     * Reset link points to the website partnerships login page with mode=reset.
     * Silent success even when email is not found (anti-enumeration).
     */
    @Transactional
    public void sendForgotPasswordEmail(String email) {
        PartnershipUser partner = partnershipUserRepository.findByEmail(email).orElse(null);
        if (partner == null) {
            // Don't reveal whether email exists
            return;
        }

        // Invalidate any previous tokens for this email
        passwordResetTokenRepository.deleteAllByEmail(email);

        // Generate token
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256Hex(rawToken);

        // Persist hashed token
        CustomerPasswordResetToken token = new CustomerPasswordResetToken(
                email, tokenHash, LocalDateTime.now().plusMinutes(15));
        passwordResetTokenRepository.save(token);

        // Build reset link → website partnerships login page
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String resetLink = websiteBaseUrl + "/partnerships/login?mode=reset&token=" + encodedToken + "&email=" + encodedEmail;

        emailService.sendPartnerPasswordResetEmail(email, partner.getFullName(), resetLink);
    }

    /**
     * Validates the reset token and updates the partner's password.
     */
    @Transactional
    public void resetPassword(String email, String rawToken, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters");
        }

        String tokenHash = sha256Hex(rawToken);
        CustomerPasswordResetToken token = passwordResetTokenRepository
                .findByEmailAndResetCodeAndUsedFalse(email, tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired. Please request a new one.");
        }

        // Mark token used
        passwordResetTokenRepository.markUsedById(token.getId());

        // Update partner password
        PartnershipUser partner = partnershipUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        partner.setPasswordHash(passwordEncoder.encode(newPassword));
        partnershipUserRepository.save(partner);
    }

    // ── Admin methods ─────────────────────────────────────────────────────────

    /**
     * Paginated search of all partners — for the portal admin view.
     * @param status         filter by status (null = all)
     * @param partnershipType filter by type (null = all)
     * @param search         text search on name/email/phone/firm
     * @param page           0-based page number
     * @param size           page size
     */
    public Page<PartnershipUser> searchPartners(String status, String partnershipType,
                                                 String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String statusParam = (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) ? null : status;
        String typeParam = (partnershipType == null || partnershipType.isBlank() || "all".equalsIgnoreCase(partnershipType)) ? null : partnershipType;
        String searchParam = (search == null || search.isBlank()) ? null : search.trim();
        return partnershipUserRepository.searchPartners(statusParam, typeParam, searchParam, pageable);
    }

    /**
     * Summary counts by status — for the admin dashboard badge counts.
     */
    public Map<String, Long> getPartnerStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("pending", partnershipUserRepository.countByStatus("pending"));
        counts.put("approved", partnershipUserRepository.countByStatus("approved"));
        counts.put("active", partnershipUserRepository.countByStatus("active"));
        counts.put("rejected", partnershipUserRepository.countByStatus("rejected"));
        counts.put("suspended", partnershipUserRepository.countByStatus("suspended"));
        counts.put("total", partnershipUserRepository.count());
        return counts;
    }

    /**
     * Full partner detail DTO for the admin view — profile + stats.
     */
    public Map<String, Object> getPartnerAdminDetail(Long partnerId) {
        PartnershipUser partner = getPartnerById(partnerId);
        Map<String, Object> stats = getPartnerStats(partnerId);

        Map<String, Object> detail = new HashMap<>();
        // Identity
        detail.put("id", partner.getId());
        detail.put("fullName", partner.getFullName());
        detail.put("email", partner.getEmail());
        detail.put("phone", partner.getPhone());
        detail.put("designation", partner.getDesignation());
        detail.put("partnershipType", partner.getPartnershipType());
        detail.put("status", partner.getStatus());
        // Business
        detail.put("firmName", partner.getFirmName());
        detail.put("companyName", partner.getCompanyName());
        detail.put("businessName", partner.getBusinessName());
        detail.put("gstNumber", partner.getGstNumber());
        detail.put("licenseNumber", partner.getLicenseNumber());
        detail.put("reraNumber", partner.getReraNumber());
        detail.put("cinNumber", partner.getCinNumber());
        detail.put("ifscCode", partner.getIfscCode());
        detail.put("employeeId", partner.getEmployeeId());
        // Professional
        detail.put("experience", partner.getExperience());
        detail.put("yearsOfPractice", partner.getYearsOfPractice());
        detail.put("specialization", partner.getSpecialization());
        detail.put("portfolioLink", partner.getPortfolioLink());
        detail.put("certifications", partner.getCertifications());
        // Operational
        detail.put("location", partner.getLocation());
        detail.put("areaOfOperation", partner.getAreaOfOperation());
        detail.put("areasCovered", partner.getAreasCovered());
        detail.put("areaServed", partner.getAreaServed());
        detail.put("landTypes", partner.getLandTypes());
        detail.put("materialsSupplied", partner.getMaterialsSupplied());
        detail.put("businessSize", partner.getBusinessSize());
        detail.put("industry", partner.getIndustry());
        detail.put("projectType", partner.getProjectType());
        detail.put("projectScale", partner.getProjectScale());
        detail.put("timeline", partner.getTimeline());
        // Additional
        detail.put("additionalContact", partner.getAdditionalContact());
        detail.put("message", partner.getMessage());
        // Timestamps
        detail.put("createdAt", partner.getCreatedAt());
        detail.put("updatedAt", partner.getUpdatedAt());
        detail.put("approvedAt", partner.getApprovedAt());
        detail.put("lastLogin", partner.getLastLogin());
        detail.put("createdBy", partner.getCreatedBy());
        detail.put("updatedBy", partner.getUpdatedBy());
        // Stats
        detail.put("stats", stats);

        return detail;
    }

    /**
     * Convert a PartnershipUser to a summary map for the admin list view.
     */
    public Map<String, Object> toAdminSummary(PartnershipUser partner) {
        List<Lead> referrals = getReferralsByPartner(partner.getId());
        long totalReferrals = referrals.size();
        long convertedReferrals = referrals.stream()
                .filter(l -> "project_won".equals(normalizeLeadStatus(l.getLeadStatus())))
                .count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("id", partner.getId());
        summary.put("fullName", partner.getFullName());
        summary.put("email", partner.getEmail());
        summary.put("phone", partner.getPhone());
        summary.put("designation", partner.getDesignation());
        summary.put("partnershipType", partner.getPartnershipType());
        summary.put("status", partner.getStatus());
        summary.put("firmName", partner.getFirmName() != null ? partner.getFirmName() : partner.getCompanyName());
        summary.put("location", partner.getLocation());
        summary.put("createdAt", partner.getCreatedAt());
        summary.put("approvedAt", partner.getApprovedAt());
        summary.put("lastLogin", partner.getLastLogin());
        summary.put("totalReferrals", totalReferrals);
        summary.put("convertedReferrals", convertedReferrals);
        return summary;
    }

    /**
     * Normalize legacy lead status variants to canonical values used by dashboards.
     */
    private String normalizeLeadStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }

        String cleaned = status.toLowerCase().trim().replaceAll("[\\s_]", "");
        if ("new".equals(cleaned) || "newinquiry".equals(cleaned)) {
            return "new_inquiry";
        }
        if ("qualified".equals(cleaned) || "qualifiedlead".equals(cleaned)) {
            return "qualified";
        }
        if ("proposalsent".equals(cleaned)) {
            return "proposal_sent";
        }
        if ("projectwon".equals(cleaned) || "won".equals(cleaned) || "converted".equals(cleaned)) {
            return "project_won";
        }
        return status.toLowerCase().trim();
    }

    /**
     * Soft-delete (suspend) a partner. Keeps data for audit trail.
     */
    @Transactional
    public void suspendPartner(Long partnerId, String updatedBy) {
        updatePartnerStatus(partnerId, "suspended", updatedBy);
    }

    // ── Referred client (friend who was referred) ────────────────────────────


    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

}
