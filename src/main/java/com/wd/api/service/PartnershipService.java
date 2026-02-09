package com.wd.api.service;

import com.wd.api.dto.PartnerLoginRequest;
import com.wd.api.dto.PartnerLoginResponse;
import com.wd.api.dto.PartnershipApplicationRequest;
import com.wd.api.model.Lead;
import com.wd.api.model.PartnershipUser;
import com.wd.api.repository.LeadRepository;
import com.wd.api.repository.PartnershipUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PartnershipService {

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

    /**
     * Partner Login
     */
    public PartnerLoginResponse login(PartnerLoginRequest request) {
        // Find user by phone
        PartnershipUser partner = partnershipUserRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new RuntimeException("Invalid phone or password"));

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), partner.getPasswordHash())) {
            throw new RuntimeException("Invalid phone or password");
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

        String token = jwtService.generatePartnerToken(partner.getPhone(), claims);

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
    @SuppressWarnings("null")
    public PartnershipUser getPartnerById(Long partnerId) {
        return partnershipUserRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found"));
    }

    /**
     * Get partner by phone number
     */
    public PartnershipUser getPartnerByPhone(String phone) {
        return partnershipUserRepository.findByPhone(phone).orElse(null);
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
    }

    /**
     * Get referral leads submitted by a specific partner.
     * Leads created via partnership referral store "Referred by Partner: {name} (ID: {partnerId})" in notes.
     */
    public List<Lead> getReferralsByPartner(Long partnerId) {
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
                .filter(l -> "new_inquiry".equals(l.getLeadStatus()) || "contacted".equals(l.getLeadStatus()))
                .count();
        long qualifiedReferrals = referrals.stream()
                .filter(l -> "qualified".equals(l.getLeadStatus()) || "proposal_sent".equals(l.getLeadStatus()))
                .count();
        long convertedReferrals = referrals.stream()
                .filter(l -> "project_won".equals(l.getLeadStatus()) || "converted".equals(l.getLeadStatus()))
                .count();
        long lostReferrals = referrals.stream()
                .filter(l -> "lost".equals(l.getLeadStatus()))
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
            summary.put("status", lead.getLeadStatus());
            summary.put("priority", lead.getPriority());
            summary.put("location", lead.getLocation());
            summary.put("budget", lead.getBudget());
            summary.put("dateOfEnquiry", lead.getDateOfEnquiry());
            summary.put("createdAt", lead.getCreatedAt());
            summaries.add(summary);
        }

        return summaries;
    }

}
