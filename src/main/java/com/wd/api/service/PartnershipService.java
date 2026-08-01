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

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_SUSPENDED = "suspended";
    private static final String STATUS_QUALIFIED = "qualified";
    private static final String STATUS_PROJECT_WON = "project_won";
    private static final String KEY_PARTNERSHIP_TYPE = "partnershipType";
    private static final String KEY_STATUS = "status";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_CREATED_AT = "createdAt";

    private final PartnershipUserRepository partnershipUserRepository;

    private final LeadRepository leadRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final String jwtSecret;

    private final CustomerPasswordResetTokenRepository passwordResetTokenRepository;

    private final EmailService emailService;

    private final String websiteBaseUrl;

    public PartnershipService(PartnershipUserRepository partnershipUserRepository,
            LeadRepository leadRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${jwt.secret}") String jwtSecret,
            CustomerPasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService,
            @Value("${app.website-base-url:https://walldotbuilders.com}") String websiteBaseUrl) {
        this.partnershipUserRepository = partnershipUserRepository;
        this.leadRepository = leadRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtSecret = jwtSecret;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.websiteBaseUrl = websiteBaseUrl;
    }

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
        if (!STATUS_ACTIVE.equals(partner.getStatus()) && !STATUS_APPROVED.equals(partner.getStatus())) {
            throw new RuntimeException("Account is not active. Status: " + partner.getStatus());
        }

        // Update last login and status to active
        partner.setLastLogin(LocalDateTime.now());
        if (STATUS_APPROVED.equals(partner.getStatus())) {
            partner.setStatus(STATUS_ACTIVE);
        }
        partnershipUserRepository.save(partner);

        // Generate JWT token with PARTNER prefix
        Map<String, Object> claims = new HashMap<>();
        claims.put("partnerId", partner.getId().toString());
        claims.put(KEY_PARTNERSHIP_TYPE, partner.getPartnershipType());
        claims.put(KEY_STATUS, partner.getStatus());

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
        partner.setStatus(STATUS_PENDING);

        // Save
        PartnershipUser savedPartner = partnershipUserRepository.save(partner);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Partnership application submitted successfully");
        response.put("partnerId", savedPartner.getId());
        response.put(KEY_STATUS, STATUS_PENDING);
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

        if (STATUS_APPROVED.equals(status)) {
            partner.setApprovedAt(LocalDateTime.now());
        }

        partnershipUserRepository.save(partner);

        // Send notification emails asynchronously
        if (STATUS_APPROVED.equals(status)) {
            emailService.sendPartnerApprovalEmail(
                    partner.getEmail(),
                    partner.getFullName(),
                    partner.getPartnershipType() != null ? partner.getPartnershipType() : "Partner");
        } else if (STATUS_REJECTED.equals(status)) {
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
                    return STATUS_QUALIFIED.equals(status) || "proposal_sent".equals(status) || "negotiation".equals(status);
                })
                .count();
        long convertedReferrals = referrals.stream()
                .filter(l -> STATUS_PROJECT_WON.equals(normalizeLeadStatus(l.getLeadStatus())))
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
            summary.put(KEY_STATUS, normalizeLeadStatus(lead.getLeadStatus()));
            summary.put("priority", lead.getPriority());
            summary.put(KEY_LOCATION, lead.getLocation());
            summary.put("budget", lead.getBudget());
            summary.put("dateOfEnquiry", lead.getDateOfEnquiry());
            summary.put(KEY_CREATED_AT, lead.getCreatedAt());
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
        Pageable pageable = PageRequest.of(page, size, Sort.by(KEY_CREATED_AT).descending());
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
        counts.put(STATUS_PENDING, partnershipUserRepository.countByStatus(STATUS_PENDING));
        counts.put(STATUS_APPROVED, partnershipUserRepository.countByStatus(STATUS_APPROVED));
        counts.put(STATUS_ACTIVE, partnershipUserRepository.countByStatus(STATUS_ACTIVE));
        counts.put(STATUS_REJECTED, partnershipUserRepository.countByStatus(STATUS_REJECTED));
        counts.put(STATUS_SUSPENDED, partnershipUserRepository.countByStatus(STATUS_SUSPENDED));
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
        detail.put(KEY_PARTNERSHIP_TYPE, partner.getPartnershipType());
        detail.put(KEY_STATUS, partner.getStatus());
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
        detail.put(KEY_LOCATION, partner.getLocation());
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
        detail.put(KEY_CREATED_AT, partner.getCreatedAt());
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
                .filter(l -> STATUS_PROJECT_WON.equals(normalizeLeadStatus(l.getLeadStatus())))
                .count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("id", partner.getId());
        summary.put("fullName", partner.getFullName());
        summary.put("email", partner.getEmail());
        summary.put("phone", partner.getPhone());
        summary.put("designation", partner.getDesignation());
        summary.put(KEY_PARTNERSHIP_TYPE, partner.getPartnershipType());
        summary.put(KEY_STATUS, partner.getStatus());
        summary.put("firmName", partner.getFirmName() != null ? partner.getFirmName() : partner.getCompanyName());
        summary.put(KEY_LOCATION, partner.getLocation());
        summary.put(KEY_CREATED_AT, partner.getCreatedAt());
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
        if (STATUS_QUALIFIED.equals(cleaned) || "qualifiedlead".equals(cleaned)) {
            return STATUS_QUALIFIED;
        }
        if ("proposalsent".equals(cleaned)) {
            return "proposal_sent";
        }
        if ("projectwon".equals(cleaned) || "won".equals(cleaned) || "converted".equals(cleaned)) {
            return STATUS_PROJECT_WON;
        }
        return status.toLowerCase().trim();
    }

    /**
     * Soft-delete (suspend) a partner. Keeps data for audit trail.
     */
    @Transactional
    public void suspendPartner(Long partnerId, String updatedBy) {
        updatePartnerStatus(partnerId, STATUS_SUSPENDED, updatedBy);
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
