package com.wd.api.service;

import com.wd.api.model.Lead;
import com.wd.api.dto.PaginationParams;
import com.wd.api.dto.PartnershipReferralRequest;
import com.wd.api.dto.LeadSearchFilter;
import com.wd.api.repository.LeadRepository;
import com.wd.api.repository.LeadQuotationRepository;
import com.wd.api.util.SpecificationBuilder;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wd.api.dto.LeadCreateRequest;

import java.security.SecureRandom;
import java.util.Base64;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LeadService {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private ActivityFeedService activityFeedService;

    @Autowired
    private LeadScoreHistoryService leadScoreHistoryService;

    @Autowired
    private com.wd.api.repository.PortalUserRepository portalUserRepository;

    @Autowired
    private com.wd.api.repository.CustomerUserRepository customerUserRepository;

    @Autowired
    private com.wd.api.repository.CustomerProjectRepository customerProjectRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private LeadQuotationRepository leadQuotationRepository;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private com.wd.api.repository.CustomerRoleRepository customerRoleRepository;

    @Autowired
    private com.wd.api.repository.LeadInteractionRepository leadInteractionRepository;

    @Autowired
    private com.wd.api.repository.ProjectMemberRepository projectMemberRepository;

    @Autowired
    private com.wd.api.repository.BoqWorkTypeRepository boqWorkTypeRepository;

    @Autowired
    private com.wd.api.repository.BoqItemRepository boqItemRepository;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LeadService.class);
    // Thread-safe ObjectMapper instance for JSON serialization
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Lead createLead(Lead lead) {
        if (lead.getDateOfEnquiry() == null) {
            lead.setDateOfEnquiry(LocalDate.now());
        }
        if (lead.getCreatedAt() == null) {
            lead.setCreatedAt(java.time.LocalDateTime.now());
        }

        // Set created by user from current authentication
        if (lead.getCreatedByUserId() == null) {
            try {
                String currentUserEmail = org.springframework.security.core.context.SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();
                com.wd.api.model.PortalUser currentUser = portalUserRepository.findByEmail(currentUserEmail)
                        .orElse(null);
                if (currentUser != null) {
                    lead.setCreatedByUserId(currentUser.getId());
                }
            } catch (Exception e) {
                logger.warn("Could not set createdByUserId: {}", e.getMessage());
            }
        }

        // Handle Assignment
        if (lead.getAssignedToId() != null) {
            Long assignedToId = lead.getAssignedToId();
            if (assignedToId != null) {
                com.wd.api.model.PortalUser user = portalUserRepository.findById(assignedToId).orElse(null);
                if (user != null) {
                    lead.setAssignedTo(user);
                    lead.setAssignedTeam(user.getFirstName() + " " + user.getLastName());
                }
            }
        }

        // Calculate Score
        calculateLeadScore(lead);

        Lead savedLead = leadRepository.save(lead);
        try {
            // Log initial score in history (previous score/category is null for new leads)
            leadScoreHistoryService.logScoreChange(
                    savedLead,
                    null, // previousScore - null for initial calculation
                    null, // previousCategory - null for initial calculation
                    savedLead.getCreatedByUserId(), // scoredById - user who created the lead
                    "Initial lead score calculation", // reason
                    savedLead.getScoreFactors() // scoreFactors
            );

            activityFeedService.logSystemActivity(
                    "LEAD_CREATED",
                    "New Lead Created",
                    "Lead " + savedLead.getName() + " was created.",
                    savedLead.getId(),
                    "LEAD");

            if (savedLead.getAssignedTo() != null) {
                activityFeedService.logSystemActivity(
                        "LEAD_ASSIGNED",
                        "Lead Assigned",
                        "Lead assigned to " + savedLead.getAssignedTeam(),
                        savedLead.getId(),
                        "LEAD");
            }

            // Send Welcome Email
            emailService.sendLeadWelcomeEmail(savedLead);
        } catch (Exception e) {
            // Log but don't fail lead creation if activity/email logging fails
            logger.error("Error in post-creation activities for lead {}: {}", savedLead.getId(), e.getMessage(), e);
        }

        return savedLead;
    }

    /**
     * Create Lead from DTO
     * Handles mapping and defaults
     */
    public Lead createLead(LeadCreateRequest request) {
        Lead lead = new Lead();
        lead.setName(request.getName());
        lead.setEmail(request.getEmail() != null ? request.getEmail() : "");
        lead.setPhone(request.getPhone() != null ? request.getPhone() : "");
        lead.setWhatsappNumber(request.getWhatsappNumber() != null ? request.getWhatsappNumber() : "");

        lead.setCustomerType(request.getCustomerType() != null && !request.getCustomerType().isEmpty()
                ? request.getCustomerType().trim().toLowerCase()
                : "other");

        lead.setProjectType(request.getProjectType() != null ? request.getProjectType() : "");
        lead.setProjectDescription(request.getProjectDescription() != null ? request.getProjectDescription() : "");
        lead.setRequirements(request.getRequirements() != null ? request.getRequirements() : "");
        lead.setBudget(request.getBudget());
        lead.setProjectSqftArea(request.getProjectSqftArea());

        String status = request.getLeadStatus();
        lead.setLeadStatus(status != null && !status.isEmpty() ? status.trim().toLowerCase().replace(' ', '_')
                : "new_inquiry");

        String source = request.getLeadSource();
        lead.setLeadSource(
                source != null && !source.isEmpty() ? source.trim().toLowerCase().replace(' ', '_') : "website");

        String priority = request.getPriority();
        lead.setPriority(priority != null && !priority.isEmpty() ? priority.trim().toLowerCase() : "low");

        lead.setAssignedTeam(request.getAssignedTeam() != null ? request.getAssignedTeam() : "");
        lead.setAssignedToId(request.getAssignedToId());
        lead.setNotes(request.getNotes() != null ? request.getNotes() : "");
        lead.setLostReason(request.getLostReason());

        lead.setClientRating(request.getClientRating() != null ? request.getClientRating() : 0);
        lead.setProbabilityToWin(request.getProbabilityToWin() != null ? request.getProbabilityToWin() : 0);
        lead.setNextFollowUp(request.getNextFollowUp());
        lead.setLastContactDate(request.getLastContactDate());

        lead.setState(request.getState());
        lead.setDistrict(request.getDistrict());
        lead.setLocation(request.getLocation());
        lead.setAddress(request.getAddress());
        lead.setPlotArea(request.getPlotArea());
        lead.setFloors(request.getFloors());

        if (request.getDateOfEnquiry() != null && !request.getDateOfEnquiry().trim().isEmpty()) {
            try {
                lead.setDateOfEnquiry(LocalDate.parse(request.getDateOfEnquiry().substring(0, 10)));
            } catch (Exception e) {
                lead.setDateOfEnquiry(LocalDate.now());
            }
        } else {
            lead.setDateOfEnquiry(LocalDate.now());
        }

        return createLead(lead);
    }

    /**
     * Update Lead from DTO
     * Uses same normalization rules as createLead(LeadCreateRequest)
     * to ensure consistency and avoid 500s from constraint violations.
     */
    @Transactional
    public Lead updateLead(Long id, com.wd.api.dto.LeadUpdateRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("Lead ID cannot be null");
        }

        // Load existing lead to support partial updates without violating NOT NULL constraints
        Lead existing = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found with id: " + id));

        Lead leadDetails = new Lead();

        // Map fields from DTO to a transient Lead details object.
        // If DTO field is null, keep the existing value (partial update semantics).
        leadDetails.setName(request.getName() != null ? request.getName() : existing.getName());
        leadDetails.setEmail(request.getEmail() != null ? request.getEmail() : existing.getEmail());
        leadDetails.setPhone(request.getPhone() != null ? request.getPhone() : existing.getPhone());
        leadDetails.setWhatsappNumber(request.getWhatsappNumber() != null
                ? request.getWhatsappNumber()
                : existing.getWhatsappNumber());

        // Customer type
        String customerType = request.getCustomerType() != null
                ? request.getCustomerType()
                : existing.getCustomerType();
        if (customerType != null && !customerType.isEmpty()) {
            leadDetails.setCustomerType(customerType.trim().toLowerCase());
        } else {
            leadDetails.setCustomerType(existing.getCustomerType());
        }

        leadDetails.setProjectType(request.getProjectType() != null ? request.getProjectType() : existing.getProjectType());
        leadDetails.setProjectDescription(request.getProjectDescription() != null
                ? request.getProjectDescription()
                : existing.getProjectDescription());
        leadDetails.setRequirements(request.getRequirements() != null
                ? request.getRequirements()
                : existing.getRequirements());
        leadDetails.setBudget(request.getBudget() != null ? request.getBudget() : existing.getBudget());
        leadDetails.setProjectSqftArea(request.getProjectSqftArea() != null
                ? request.getProjectSqftArea()
                : existing.getProjectSqftArea());

        // Status
        String status = request.getLeadStatus() != null ? request.getLeadStatus() : existing.getLeadStatus();
        if (status != null && !status.isEmpty()) {
            leadDetails.setLeadStatus(status.trim().toLowerCase().replace(' ', '_'));
        } else {
            leadDetails.setLeadStatus(existing.getLeadStatus());
        }

        // Source
        String source = request.getLeadSource() != null ? request.getLeadSource() : existing.getLeadSource();
        if (source != null && !source.isEmpty()) {
            leadDetails.setLeadSource(source.trim().toLowerCase().replace(' ', '_'));
        } else {
            leadDetails.setLeadSource(existing.getLeadSource());
        }

        // Priority
        String priority = request.getPriority() != null ? request.getPriority() : existing.getPriority();
        if (priority != null && !priority.isEmpty()) {
            leadDetails.setPriority(priority.trim().toLowerCase());
        } else {
            leadDetails.setPriority(existing.getPriority());
        }

        leadDetails.setAssignedTeam(request.getAssignedTeam() != null
                ? request.getAssignedTeam()
                : existing.getAssignedTeam());
        leadDetails.setState(request.getState() != null ? request.getState() : existing.getState());
        leadDetails.setDistrict(request.getDistrict() != null ? request.getDistrict() : existing.getDistrict());
        leadDetails.setLocation(request.getLocation() != null ? request.getLocation() : existing.getLocation());
        leadDetails.setAddress(existing.getAddress()); // address not in DTO; keep existing

        leadDetails.setNotes(request.getNotes() != null ? request.getNotes() : existing.getNotes());
        leadDetails.setClientRating(request.getClientRating() != null
                ? request.getClientRating()
                : existing.getClientRating());
        leadDetails.setProbabilityToWin(request.getProbabilityToWin() != null
                ? request.getProbabilityToWin()
                : existing.getProbabilityToWin());
        leadDetails.setNextFollowUp(request.getNextFollowUp() != null
                ? request.getNextFollowUp()
                : existing.getNextFollowUp());
        leadDetails.setLastContactDate(request.getLastContactDate() != null
                ? request.getLastContactDate()
                : existing.getLastContactDate());
        leadDetails.setDateOfEnquiry(request.getDateOfEnquiry() != null
                ? request.getDateOfEnquiry()
                : existing.getDateOfEnquiry());

        // Assignment via transient assignedToId: keep existing if not provided
        Long newAssignedToId = request.getAssignedToId() != null
                ? request.getAssignedToId()
                : existing.getAssignedToId();
        leadDetails.setAssignedToId(newAssignedToId);

        return updateLead(id, leadDetails);
    }

    @Transactional
    public Lead updateLead(Long id, Lead leadDetails) {
        if (id == null) {
            throw new IllegalArgumentException("Lead ID cannot be null");
        }
        if (leadDetails == null) {
            throw new IllegalArgumentException("Lead details cannot be null");
        }
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found with id: " + id));
        try {
            String oldStatus = lead.getLeadStatus();
            String oldCategory = lead.getScoreCategory(); // Capture old score category
            Integer oldScore = lead.getScore(); // Capture old score
            String oldAssigned = lead.getAssignedTeam();
            Long oldAssignedId = lead.getAssignedTo() != null ? lead.getAssignedTo().getId() : null;

            lead.setName(leadDetails.getName());
            lead.setEmail(leadDetails.getEmail());
            lead.setPhone(leadDetails.getPhone());
            lead.setWhatsappNumber(leadDetails.getWhatsappNumber());
            lead.setLeadSource(leadDetails.getLeadSource());
            // Validate status transition before updating (Enterprise business rules)
            if (leadDetails.getLeadStatus() != null &&
                    !leadDetails.getLeadStatus().equals(oldStatus)) {
                validateLeadStatusTransition(oldStatus, leadDetails.getLeadStatus());
            }
            lead.setLeadStatus(leadDetails.getLeadStatus());
            lead.setPriority(leadDetails.getPriority());
            lead.setCustomerType(leadDetails.getCustomerType());
            lead.setProjectType(leadDetails.getProjectType());
            lead.setProjectDescription(leadDetails.getProjectDescription());
            lead.setRequirements(leadDetails.getRequirements());
            lead.setBudget(leadDetails.getBudget());
            lead.setProjectSqftArea(leadDetails.getProjectSqftArea());
            // Handle date fields with null safety
            lead.setNextFollowUp(leadDetails.getNextFollowUp());
            lead.setLastContactDate(leadDetails.getLastContactDate());
            lead.setDateOfEnquiry(leadDetails.getDateOfEnquiry());
            lead.setState(leadDetails.getState());
            lead.setDistrict(leadDetails.getDistrict());
            lead.setLocation(leadDetails.getLocation());
            lead.setAddress(leadDetails.getAddress());
            lead.setNotes(leadDetails.getNotes());
            lead.setClientRating(leadDetails.getClientRating());
            lead.setProbabilityToWin(leadDetails.getProbabilityToWin());
            lead.setLostReason(leadDetails.getLostReason());

            // Handle Assignment Update - Priority: assignedToId > assignedTo > assignedTeam
            // assignedToId is a @Transient field used for JSON deserialization
            // Enhanced with null safety and detailed logging
            Long newAssignedToId = leadDetails.getAssignedToId();
            logger.debug("Updating lead {} assignment - newAssignedToId: {}, oldAssignedId: {}",
                    id, newAssignedToId, oldAssignedId);

            if (newAssignedToId != null) {
                // New assignment or change in assignment
                if (oldAssignedId == null || !newAssignedToId.equals(oldAssignedId)) {
                    try {
                        com.wd.api.model.PortalUser user = portalUserRepository.findById(newAssignedToId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Assigned user not found with id: " + newAssignedToId));
                        lead.setAssignedTo(user);
                        // Note: assignedToId is @Transient, so setting it doesn't persist, but it's
                        // useful for JSON serialization
                        lead.setAssignedTeam(user.getFirstName() + " " + user.getLastName());
                        logger.info("Lead {} assigned to user {} ({})", id, newAssignedToId, lead.getAssignedTeam());
                    } catch (IllegalArgumentException e) {
                        logger.error("Failed to assign lead {} to user {}: {}", id, newAssignedToId, e.getMessage());
                        throw e;
                    }
                }
            } else if (leadDetails.getAssignedTo() != null && leadDetails.getAssignedTo().getId() != null) {
                // If assignedTo entity is provided directly (less common)
                Long assignedToEntityId = leadDetails.getAssignedTo().getId();
                if (oldAssignedId == null || !assignedToEntityId.equals(oldAssignedId)) {
                    lead.setAssignedTo(leadDetails.getAssignedTo());
                    lead.setAssignedTeam(leadDetails.getAssignedTo().getFirstName() + " "
                            + leadDetails.getAssignedTo().getLastName());
                    logger.info("Lead {} assigned to user {} via assignedTo entity", id, assignedToEntityId);
                }
            } else {
                // Unassign lead if both assignedToId and assignedTo are explicitly null
                // This handles the case where user wants to remove assignment
                if (oldAssignedId != null) {
                    lead.setAssignedTo(null);
                    // Keep assignedTeam as is if provided, otherwise set to null
                    if (leadDetails.getAssignedTeam() != null && !leadDetails.getAssignedTeam().isEmpty()) {
                        lead.setAssignedTeam(leadDetails.getAssignedTeam());
                    } else {
                        lead.setAssignedTeam(null);
                    }
                    logger.info("Lead {} unassigned (was assigned to user {})", id, oldAssignedId);
                }
            }

            // Recalculate Score on Update
            // Wrap in try-catch to prevent score calculation errors from breaking updates
            try {
                calculateLeadScore(lead);
            } catch (Exception e) {
                logger.error("Error calculating lead score for lead {}: {}", id, e.getMessage(), e);
                // Set default score values but don't fail the update
                lead.setScore(lead.getScore() != null ? lead.getScore() : 0);
                lead.setScoreCategory(lead.getScoreCategory() != null ? lead.getScoreCategory() : "COLD");
            }

            lead.setUpdatedAt(java.time.LocalDateTime.now());

            // Save the lead - this is the critical operation
            Lead savedLead;
            try {
                savedLead = leadRepository.save(lead);
            } catch (Exception e) {
                logger.error("Error saving lead {}: {}", id, e.getMessage(), e);
                throw new RuntimeException("Failed to save lead: " + e.getMessage(), e);
            }

            // Log score change in history if score or category changed
            if (!java.util.Objects.equals(oldScore, savedLead.getScore()) ||
                    !java.util.Objects.equals(oldCategory, savedLead.getScoreCategory())) {
                try {
                    String reason = "Lead updated - automatic score recalculation";
                    if (!java.util.Objects.equals(oldScore, savedLead.getScore())) {
                        reason = String.format("Score changed from %d to %d",
                                oldScore != null ? oldScore : 0,
                                savedLead.getScore() != null ? savedLead.getScore() : 0);
                    }

                    leadScoreHistoryService.logScoreChange(
                            savedLead,
                            oldScore, // previousScore
                            oldCategory, // previousCategory
                            savedLead.getUpdatedByUserId(), // scoredById - user who updated the lead
                            reason,
                            savedLead.getScoreFactors() // scoreFactors - new factors
                    );
                } catch (Exception e) {
                    logger.warn("Error logging score change for lead {}: {}", savedLead.getId(), e.getMessage());
                    // Don't throw - score logging should not break lead updates
                }
            }

            try {
                activityFeedService.logSystemActivity(
                        "LEAD_UPDATED",
                        "Lead Updated",
                        "Lead " + savedLead.getName() + " was updated.",
                        savedLead.getId(),
                        "LEAD");

                if (savedLead.getLeadStatus() != null && !savedLead.getLeadStatus().equals(oldStatus)) {
                    activityFeedService.logSystemActivity(
                            "LEAD_STATUS_CHANGED",
                            "Lead Status Changed",
                            "Status changed from " + oldStatus + " to " + savedLead.getLeadStatus(),
                            savedLead.getId(),
                            "LEAD");

                    // Send Status Update Email
                    emailService.sendLeadStatusUpdateEmail(savedLead, oldStatus, savedLead.getLeadStatus());
                }

                // Check if Lead became HOT
                if ("HOT".equals(savedLead.getScoreCategory()) && !"HOT".equals(oldCategory)) {
                    emailService.sendAdminScoreAlert(savedLead);
                }

                boolean assignedChanged = false;
                if (savedLead.getAssignedTo() != null
                        && (oldAssignedId == null || !savedLead.getAssignedTo().getId().equals(oldAssignedId))) {
                    assignedChanged = true;
                } else if (savedLead.getAssignedTeam() != null && !savedLead.getAssignedTeam().equals(oldAssigned)) {
                    assignedChanged = true;
                }

                if (assignedChanged) {
                    activityFeedService.logSystemActivity(
                            "LEAD_ASSIGNED",
                            "Lead Assigned",
                            "Lead assigned to " + savedLead.getAssignedTeam(),
                            savedLead.getId(),
                            "LEAD");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return savedLead;
        } catch (IllegalArgumentException e) {
            // Re-throw IllegalArgumentException as-is (for validation errors)
            throw e;
        } catch (IllegalStateException e) {
            // Re-throw IllegalStateException as-is (for business rule violations)
            throw e;
        } catch (Exception e) {
            // Wrap unexpected exceptions with more context
            logger.error("Unexpected error updating lead {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to update lead: " + e.getMessage(), e);
        }
    }

    @Transactional
    public boolean deleteLead(Long id) {
        if (id != null && leadRepository.existsById(id)) {
            leadRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<com.wd.api.dto.ActivityFeedDTO> getLeadActivities(Long leadId) {
        return activityFeedService.getActivitiesForLead(leadId);
    }

    public Lead getLeadById(Long id) {
        if (id == null)
            return null;
        return leadRepository.findById(id).orElse(null);
    }

    public List<Lead> getAllLeads() {
        return leadRepository.findAll();
    }

    /**
     * NEW: Standardized search method using LeadSearchFilter
     * Enterprise-grade implementation with SpecificationBuilder
     */
    public Page<Lead> search(LeadSearchFilter filter) {
        Specification<Lead> spec = buildSearchSpecification(filter);
        return leadRepository.findAll(spec, filter.toPageable());
    }

    /**
     * Build JPA Specification from LeadSearchFilter
     * Uses SpecificationBuilder for clean, reusable logic
     */
    private Specification<Lead> buildSearchSpecification(LeadSearchFilter filter) {
        SpecificationBuilder<Lead> builder = new SpecificationBuilder<>();

        // Search across multiple fields
        Specification<Lead> searchSpec = builder.buildSearch(
                filter.getSearchQuery(),
                "name", "email", "phone", "whatsappNumber", "projectDescription");

        // Apply filters with status normalization
        Specification<Lead> statusSpec = null;
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            final String normalizedStatus = normalizeStatusForComparison(filter.getStatus());
            statusSpec = (root, query, cb) -> {
                // Normalize database column value (remove spaces/underscores, lowercase)
                jakarta.persistence.criteria.Expression<String> dbStatusLower = cb.lower(root.get("leadStatus"));
                jakarta.persistence.criteria.Expression<String> dbStatusNoSpaces = cb.function(
                        "REPLACE", String.class, dbStatusLower, cb.literal(" "), cb.literal(""));
                jakarta.persistence.criteria.Expression<String> dbStatusCleaned = cb.function(
                        "REPLACE", String.class, dbStatusNoSpaces, cb.literal("_"), cb.literal(""));

                // Match all variations that normalize to the same value
                // For "new": match "newinquiry", "new"
                // For "qualified": match "qualifiedlead", "qualified"
                // etc.
                List<Predicate> statusPredicates = new ArrayList<>();
                if ("new".equals(normalizedStatus)) {
                    statusPredicates.add(cb.equal(dbStatusCleaned, "newinquiry"));
                    statusPredicates.add(cb.equal(dbStatusCleaned, "new"));
                } else if ("qualified".equals(normalizedStatus)) {
                    statusPredicates.add(cb.equal(dbStatusCleaned, "qualifiedlead"));
                    statusPredicates.add(cb.equal(dbStatusCleaned, "qualified"));
                } else if ("proposal_sent".equals(normalizedStatus)) {
                    statusPredicates.add(cb.equal(dbStatusCleaned, "proposalsent"));
                } else if ("won".equals(normalizedStatus)) {
                    statusPredicates.add(cb.equal(dbStatusCleaned, "projectwon"));
                    statusPredicates.add(cb.equal(dbStatusCleaned, "won"));
                    statusPredicates.add(cb.equal(dbStatusCleaned, "converted"));
                } else {
                    // For other statuses, match the normalized value directly
                    statusPredicates.add(cb.equal(dbStatusCleaned, normalizedStatus));
                }

                return cb.or(statusPredicates.toArray(new Predicate[0]));
            };
        }
        Specification<Lead> sourceSpec = builder.buildEquals("leadSource", filter.getSource());
        Specification<Lead> prioritySpec = builder.buildEquals("priority", filter.getPriority());
        Specification<Lead> customerTypeSpec = builder.buildEquals("customerType", filter.getCustomerType());
        Specification<Lead> projectTypeSpec = builder.buildEquals("projectType", filter.getProjectType());
        Specification<Lead> stateSpec = builder.buildEquals("state", filter.getState());
        Specification<Lead> districtSpec = builder.buildEquals("district", filter.getDistrict());

        // Assigned team filter (handles both ID and name)
        Specification<Lead> assignedSpec = null;
        if (filter.getAssignedTeam() != null && !filter.getAssignedTeam().trim().isEmpty()) {
            try {
                Long assignedId = Long.parseLong(filter.getAssignedTeam());
                assignedSpec = (root, query, cb) -> cb.equal(root.get("assignedTo").get("id"), assignedId);
            } catch (NumberFormatException e) {
                assignedSpec = builder.buildEquals("assignedTeam", filter.getAssignedTeam());
            }
        }

        // Budget range
        Specification<Lead> budgetSpec = builder.buildNumericRange(
                "budget",
                filter.getMinBudget(),
                filter.getMaxBudget());

        // Date range (on createdAt)
        Specification<Lead> dateRangeSpec = null;
        if (filter.getStartDate() != null || filter.getEndDate() != null) {
            dateRangeSpec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (filter.getStartDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            root.get("createdAt"),
                            filter.getStartDate().atStartOfDay()));
                }
                if (filter.getEndDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            root.get("createdAt"),
                            filter.getEndDate().plusDays(1).atStartOfDay()));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            };
        }

        // Combine all specifications with AND logic
        return builder.and(
                searchSpec,
                statusSpec,
                sourceSpec,
                prioritySpec,
                customerTypeSpec,
                projectTypeSpec,
                assignedSpec,
                stateSpec,
                districtSpec,
                budgetSpec,
                dateRangeSpec);
    }

    /**
     * DEPRECATED: Old pagination method - kept for backward compatibility
     * Use search(LeadSearchFilter) instead
     */
    @Deprecated
    public Page<Lead> getLeadsPaginated(PaginationParams params) {
        String sortOrder = params.getSortOrder() != null ? params.getSortOrder() : "DESC";
        String sortBy = params.getSortBy() != null ? params.getSortBy() : "createdAt";
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), mapSortField(sortBy));
        int pageZeroBased = Math.max(0, params.getPage() - 1);
        Pageable pageable = PageRequest.of(pageZeroBased, params.getLimit(), sort);

        Specification<Lead> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (params.getStatus() != null && !params.getStatus().isEmpty()) {
                // Normalize input status for comparison
                String normalizedInput = normalizeStatusForComparison(params.getStatus());

                // Normalize database column value using SQL functions (remove
                // spaces/underscores, lowercase)
                // This handles "New Inquiry", "new_inquiry", "new" all matching correctly
                jakarta.persistence.criteria.Expression<String> dbStatusLower = cb.lower(root.get("leadStatus"));
                jakarta.persistence.criteria.Expression<String> dbStatusNoSpaces = cb.function(
                        "REPLACE", String.class, dbStatusLower, cb.literal(" "), cb.literal(""));
                jakarta.persistence.criteria.Expression<String> dbStatusCleaned = cb.function(
                        "REPLACE", String.class, dbStatusNoSpaces, cb.literal("_"), cb.literal(""));

                // Match all variations that normalize to the same value
                // For "new": match "newinquiry", "new"
                // For "qualified": match "qualifiedlead", "qualified"
                // etc.
                List<Predicate> statusPredicates = new ArrayList<>();
                if ("new".equals(normalizedInput)) {
                    statusPredicates.add(cb.equal(dbStatusCleaned, "newinquiry"));
                    statusPredicates.add(cb.equal(dbStatusCleaned, "new"));
                } else if ("qualified".equals(normalizedInput)) {
                    statusPredicates.add(cb.equal(dbStatusCleaned, "qualifiedlead"));
                    statusPredicates.add(cb.equal(dbStatusCleaned, "qualified"));
                } else if ("proposal_sent".equals(normalizedInput)) {
                    statusPredicates.add(cb.equal(dbStatusCleaned, "proposalsent"));
                } else if ("won".equals(normalizedInput)) {
                    statusPredicates.add(cb.equal(dbStatusCleaned, "projectwon"));
                    statusPredicates.add(cb.equal(dbStatusCleaned, "won"));
                    statusPredicates.add(cb.equal(dbStatusCleaned, "converted"));
                } else {
                    // For other statuses, match the normalized value directly
                    statusPredicates.add(cb.equal(dbStatusCleaned, normalizedInput));
                }

                predicates.add(cb.or(statusPredicates.toArray(new Predicate[0])));
            }
            if (params.getSource() != null && !params.getSource().isEmpty()) {
                predicates.add(cb.equal(root.get("leadSource"), params.getSource()));
            }
            if (params.getPriority() != null && !params.getPriority().isEmpty()) {
                predicates.add(cb.equal(root.get("priority"), params.getPriority()));
            }
            if (params.getCustomerType() != null && !params.getCustomerType().isEmpty()) {
                predicates.add(cb.equal(root.get("customerType"), params.getCustomerType()));
            }
            if (params.getProjectType() != null && !params.getProjectType().isEmpty()) {
                predicates.add(cb.equal(root.get("projectType"), params.getProjectType()));
            }

            if (params.getSearch() != null && !params.getSearch().trim().isEmpty()) {
                String searchPattern = params.getSearch().trim();
                String likePattern = "%" + searchPattern.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likePattern),
                        cb.like(cb.lower(root.get("email")), likePattern),
                        cb.like(cb.lower(root.get("phone")), likePattern)));
            }

            if (params.getAssignedTeam() != null && !params.getAssignedTeam().isEmpty()) {
                try {
                    Long id = Long.parseLong(params.getAssignedTeam());
                    predicates.add(cb.equal(root.get("assignedTo").get("id"), id));
                } catch (NumberFormatException e) {
                    predicates.add(cb.equal(root.get("assignedTeam"), params.getAssignedTeam()));
                }
            }

            if (params.getState() != null && !params.getState().isEmpty()) {
                predicates.add(cb.equal(root.get("state"), params.getState()));
            }
            if (params.getDistrict() != null && !params.getDistrict().isEmpty()) {
                predicates.add(cb.equal(root.get("district"), params.getDistrict()));
            }
            if (params.getLocation() != null && !params.getLocation().isEmpty()) {
                predicates.add(cb.equal(root.get("location"), params.getLocation()));
            }
            if (params.getMinBudget() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("budget"), params.getMinBudget()));
            }
            if (params.getMaxBudget() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("budget"), params.getMaxBudget()));
            }
            if (params.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), params.getStartDate().atStartOfDay()));
            }
            if (params.getEndDate() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(root.get("createdAt"), params.getEndDate().plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return leadRepository.findAll(spec, pageable);
    }

    public Map<String, Object> getLeadAnalytics() {
        Map<String, Object> analytics = new java.util.HashMap<>();
        Map<String, Long> statusDist = new java.util.HashMap<>();
        List<String> statuses = List.of("new", "contacted", "qualified", "proposal_sent", "won", "lost");
        for (String status : statuses) {
            statusDist.put(status, leadRepository.countByLeadStatus(status));
        }
        analytics.put("statusDistribution", statusDist);

        List<Object[]> sourceCounts = leadRepository.countLeadsBySource();
        Map<String, Long> sourceDist = new HashMap<>();
        for (Object[] row : sourceCounts) {
            String source = (String) row[0];
            Long count = (Long) row[1];
            sourceDist.put(source != null ? source : "unknown", count);
        }
        analytics.put("sourceDistribution", sourceDist);

        List<Object[]> priorityCounts = leadRepository.countLeadsByPriority();
        Map<String, Long> priorityDist = new HashMap<>();
        for (Object[] row : priorityCounts) {
            String priority = (String) row[0];
            Long count = (Long) row[1];
            priorityDist.put(priority != null ? priority : "unknown", count);
        }
        analytics.put("priorityDistribution", priorityDist);

        analytics.put("monthlyTrends", "Use custom @Query for monthly trend analysis");
        return analytics;
    }

    public Map<String, Object> getLeadConversionMetrics() {
        Map<String, Object> metrics = new java.util.HashMap<>();
        long totalLeads = leadRepository.count();
        metrics.put("totalLeads", totalLeads);
        long convertedLeads = leadRepository.countByLeadStatus("won");
        metrics.put("convertedLeads", convertedLeads);

        if (totalLeads > 0) {
            double conversionRate = (convertedLeads * 100.0) / totalLeads;
            metrics.put("conversionRate", Math.round(conversionRate * 100.0) / 100.0);
        } else {
            metrics.put("conversionRate", 0.0);
        }
        return metrics;
    }

    public List<Lead> getOverdueFollowUps() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<String> excludedStatuses = List.of("won", "lost");
        return leadRepository.findByNextFollowUpBeforeAndLeadStatusNotIn(now, excludedStatuses);
    }

    public List<Lead> getLeadsByStatus(String status) {
        return leadRepository.findByLeadStatus(status);
    }

    public List<Lead> getLeadsBySource(String source) {
        return leadRepository.findByLeadSource(source);
    }

    public List<Lead> getLeadsByPriority(String priority) {
        return leadRepository.findByPriority(priority);
    }

    public List<Lead> getLeadsByAssignedTo(String teamIdLike) {
        try {
            Long id = Long.parseLong(teamIdLike);
            return leadRepository.findByAssignedTo_Id(id);
        } catch (NumberFormatException e) {
            return leadRepository.findByAssignedTeam(teamIdLike);
        }
    }

    public List<Lead> searchLeads(String query) {
        return leadRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCase(
                query, query, query);
    }

    public Lead createLeadFromPartnershipReferral(PartnershipReferralRequest request) {
        Lead lead = new Lead();
        lead.setName(request.getClientName());
        lead.setEmail(request.getClientEmail());
        lead.setPhone(request.getClientPhone());
        lead.setWhatsappNumber(request.getClientWhatsapp());
        lead.setLeadSource("referral_architect");
        lead.setLeadStatus("new_inquiry");
        lead.setCustomerType("individual");
        lead.setPriority("medium");
        lead.setProjectType(request.getProjectType());
        lead.setProjectDescription(request.getProjectDescription());
        lead.setBudget(request.getEstimatedBudget());
        lead.setState(request.getState());
        lead.setDistrict(request.getDistrict());
        lead.setLocation(request.getLocation());
        lead.setDateOfEnquiry(LocalDate.now());

        String partnerNotes = String.format("Referred by Partner: %s (ID: %s)", request.getPartnerName(),
                request.getPartnerId());
        lead.setNotes(partnerNotes + (request.getNotes() != null ? "\n" + request.getNotes() : ""));

        return leadRepository.save(lead);
    }

    /**
     * Normalize status for comparison by removing spaces/underscores and mapping to
     * standard values
     * This ensures "New Inquiry", "new_inquiry", and "new" all normalize to "new"
     * Used for comparing database values with input values
     */
    /**
     * Validate Lead status transitions (Enterprise business rules for Construction
     * CRM)
     * 
     * Valid transitions:
     * - NEW_INQUIRY → CONTACTED → QUALIFIED → PROPOSAL_SENT → PROJECT_WON/LOST
     * - Any → LOST (terminal state - cannot change)
     * - PROJECT_WON → Cannot change (terminal state - converted)
     * - LOST → Cannot change (terminal state)
     * 
     * @param fromStatus Current lead status
     * @param toStatus   New lead status to transition to
     * @throws IllegalStateException if transition is invalid
     */
    private void validateLeadStatusTransition(String fromStatus, String toStatus) {
        if (fromStatus == null || toStatus == null) {
            return; // Allow null status changes (handled separately)
        }

        // Normalize status strings for comparison
        String fromNormalized = normalizeStatusForComparison(fromStatus);
        String toNormalized = normalizeStatusForComparison(toStatus);

        // Terminal states - cannot transition from these
        if (fromNormalized.equals("lost") ||
                fromNormalized.equals("won") ||
                fromNormalized.equals("converted") ||
                fromNormalized.equals("projectwon")) {
            throw new IllegalStateException(
                    String.format("Cannot change status from '%s' (terminal state). Lead is %s.",
                            fromStatus,
                            fromNormalized.equals("lost") ? "lost" : "already converted"));
        }

        // Terminal states - cannot transition to these without proper flow
        // LOST can be set from any non-terminal state
        if (toNormalized.equals("lost")) {
            // Allowed from any non-terminal state
            return;
        }

        // PROJECT_WON/CONVERTED can only be set from PROPOSAL_SENT or QUALIFIED
        if (toNormalized.equals("won") ||
                toNormalized.equals("converted") ||
                toNormalized.equals("projectwon")) {
            if (!fromNormalized.equals("proposal_sent") &&
                    !fromNormalized.equals("qualified")) {
                throw new IllegalStateException(
                        String.format(
                                "Cannot transition from '%s' to '%s'. Lead must be in PROPOSAL_SENT or QUALIFIED status first.",
                                fromStatus, toStatus));
            }
            return;
        }

        // Valid progression transitions
        boolean isValidTransition = false;

        switch (fromNormalized) {
            case "new":
            case "newinquiry":
                // NEW_INQUIRY can transition to CONTACTED, QUALIFIED (skip contacted), or LOST
                isValidTransition = (toNormalized.equals("contacted") ||
                        toNormalized.equals("qualified") ||
                        toNormalized.equals("proposal_sent")); // Allow skipping contacted
                break;

            case "contacted":
                // CONTACTED can transition to QUALIFIED, PROPOSAL_SENT (skip qualified), or
                // LOST
                isValidTransition = (toNormalized.equals("qualified") ||
                        toNormalized.equals("proposal_sent"));
                break;

            case "qualified":
                // QUALIFIED can transition to PROPOSAL_SENT, or LOST
                isValidTransition = (toNormalized.equals("proposal_sent"));
                break;

            case "proposal_sent":
                // PROPOSAL_SENT can transition to PROJECT_WON/CONVERTED, or LOST
                isValidTransition = (toNormalized.equals("won") ||
                        toNormalized.equals("converted") ||
                        toNormalized.equals("projectwon"));
                break;

            default:
                // Unknown from status - allow transition but log warning
                logger.warn("Unknown lead status '{}' in transition to '{}'", fromStatus, toStatus);
                isValidTransition = true; // Allow unknown statuses for flexibility
                break;
        }

        if (!isValidTransition) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from '%s' to '%s'. Valid transitions: %s",
                            fromStatus,
                            toStatus,
                            getValidTransitions(fromNormalized)));
        }
    }

    /**
     * Get valid transitions for a given status (for error messages)
     */
    private String getValidTransitions(String normalizedStatus) {
        switch (normalizedStatus) {
            case "new":
            case "newinquiry":
                return "CONTACTED, QUALIFIED, PROPOSAL_SENT, LOST";
            case "contacted":
                return "QUALIFIED, PROPOSAL_SENT, LOST";
            case "qualified":
                return "PROPOSAL_SENT, LOST";
            case "proposalsent":
                return "PROJECT_WON/CONVERTED, LOST";
            default:
                return "Consult business rules";
        }
    }

    private String normalizeStatusForComparison(String status) {
        if (status == null || status.isEmpty()) {
            return status;
        }
        // Remove spaces and underscores, convert to lowercase
        String cleaned = status.toLowerCase().trim().replaceAll("[\\s_]", "");

        // Map to standard values
        if (cleaned.equals("newinquiry") || cleaned.equals("new")) {
            return "new";
        } else if (cleaned.equals("contacted")) {
            return "contacted";
        } else if (cleaned.equals("qualifiedlead") || cleaned.equals("qualified")) {
            return "qualified";
        } else if (cleaned.equals("proposalsent")) {
            return "proposal_sent";
        } else if (cleaned.equals("negotiation")) {
            return "negotiation";
        } else if (cleaned.equals("projectwon") || cleaned.equals("won") || cleaned.equals("converted")) {
            return "won";
        } else if (cleaned.equals("lost")) {
            return "lost";
        } else {
            // Return cleaned version if no mapping found
            return cleaned;
        }
    }

    private String mapSortField(String dbColumn) {
        if (dbColumn == null || dbColumn.isEmpty()) {
            return "createdAt";
        }
        switch (dbColumn) {
            case "created_at":
                return "createdAt";
            case "updated_at":
                return "updatedAt";
            case "lead_id":
                return "id";
            case "id":
                return "id";
            case "date_of_enquiry":
                return "dateOfEnquiry";
            case "lead_status":
                return "leadStatus";
            case "lead_source":
                return "leadSource";
            case "customer_type":
                return "customerType";
            case "project_type":
                return "projectType";
            case "client_rating":
                return "clientRating";
            case "probability_to_win":
                return "probabilityToWin";
            case "next_follow_up":
                return "nextFollowUp";
            case "last_contact_date":
                return "lastContactDate";
            case "assigned_team":
                return "assignedTeam";
            case "project_sqft_area":
                return "projectSqftArea";
            default:
                return dbColumn;
        }
    }

    @Transactional
    public com.wd.api.model.CustomerProject convertLead(Long leadId, com.wd.api.dto.LeadConversionRequest request,
            String username) {
        if (leadId == null)
            throw new IllegalArgumentException("Lead ID cannot be null");

        try {
            Lead lead = leadRepository.findById(leadId)
                    .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));

            com.wd.api.model.PortalUser convertedBy = portalUserRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + username));

            // Validate Status
            if ("WON".equalsIgnoreCase(lead.getLeadStatus()) || "converted".equalsIgnoreCase(lead.getLeadStatus())) {
                throw new IllegalStateException("Lead is already converted to a project");
            }
            if ("LOST".equalsIgnoreCase(lead.getLeadStatus()) || "lost".equalsIgnoreCase(lead.getLeadStatus())) {
                throw new IllegalArgumentException("Cannot convert a lost lead. Please update lead status first.");
            }

            // Check for duplicate conversion
            if (customerProjectRepository.existsByLeadId(leadId)) {
                throw new IllegalStateException("This lead has already been converted to a project");
            }

            // 1. Create or Find Customer User
            // CustomerUser requires email (NOT NULL and UNIQUE), so validate lead has email
            if (lead.getEmail() == null || lead.getEmail().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Cannot convert lead without email address. Please update lead with a valid email first.");
            }

            com.wd.api.model.CustomerUser customer = customerUserRepository.findByEmail(lead.getEmail())
                    .orElseGet(() -> createCustomerFromLead(lead));

            // 2. Create Project
            com.wd.api.model.CustomerProject project = new com.wd.api.model.CustomerProject();
            project.setName(request.getProjectName() != null ? request.getProjectName() : lead.getName() + " Project");
            project.setCustomer(customer);
            project.setLeadId(lead.getId());
            project.setStartDate(request.getStartDate() != null ? request.getStartDate() : java.time.LocalDate.now());
            project.setProjectPhase(com.wd.api.model.enums.ProjectPhase.PLANNING);
            project.setState(lead.getState());
            project.setDistrict(lead.getDistrict());
            project.setLocation(request.getLocation() != null ? request.getLocation() : lead.getLocation());
            project.setSqfeet(lead.getProjectSqftArea());
            project.setProjectType(request.getProjectType());
            project.setPlotArea(lead.getPlotArea());
            project.setFloors(lead.getFloors());
            project.setProjectDescription(lead.getProjectDescription());

            // Set Budget
            if (request.getQuotationId() != null) {
                Long quoteId = request.getQuotationId();
                com.wd.api.model.LeadQuotation quote = leadQuotationRepository.findById(quoteId)
                        .orElseThrow(() -> new IllegalArgumentException("Quotation not found: " + quoteId));

                if (!quote.getLeadId().equals(lead.getId())) {
                    throw new IllegalArgumentException("Quotation does not belong to this lead");
                }

                project.setBudget(quote.getFinalAmount());
                project.setDesignPackage(quote.getTitle());
                quote.setStatus("ACCEPTED");
                quote.setRespondedAt(java.time.LocalDateTime.now());
                leadQuotationRepository.save(quote);
            } else {
                project.setBudget(lead.getBudget());
            }

            com.wd.api.model.CustomerProject savedProject = customerProjectRepository.save(project);

            // Generate Code
            String projectCode = "PRJ-" + java.time.LocalDate.now().getYear() + "-"
                    + String.format("%04d", savedProject.getId());
            savedProject.setCode(projectCode);
            savedProject.setConvertedById(convertedBy.getId());
            savedProject.setConvertedFromLeadId(lead.getId());
            savedProject.setConvertedAt(java.time.LocalDateTime.now());
            savedProject = customerProjectRepository.save(savedProject);

            // 4. Assign PM
            if (request.getProjectManagerId() != null) {
                Long pmId = request.getProjectManagerId();
                com.wd.api.model.PortalUser pmUser = portalUserRepository.findById(pmId).orElse(null);
                if (pmUser != null) {
                    savedProject.setProjectManager(pmUser);
                    savedProject = customerProjectRepository.save(savedProject);

                    com.wd.api.model.ProjectMember pmMember = new com.wd.api.model.ProjectMember();
                    pmMember.setProject(savedProject);
                    pmMember.setPortalUser(pmUser);
                    pmMember.setRoleInProject("PROJECT_MANAGER");
                    projectMemberRepository.save(pmMember);
                }
            }

            // 5. Migrate Items & Docs
            if (request.getQuotationId() != null) {
                migrateQuotationToBoq(request.getQuotationId(), savedProject);
            }
            documentService.migrateLeadDocumentsToProject(lead.getId(), savedProject.getId());
            activityFeedService.linkLeadActivitiesToProject(lead.getId(), savedProject);

            // 6. Finalize Lead
            lead.setLeadStatus("WON");
            lead.setConvertedById(convertedBy.getId());
            lead.setConvertedAt(java.time.LocalDateTime.now());
            leadRepository.save(lead);

            try {
                activityFeedService.logProjectActivity("LEAD_CONVERTED", "Lead Converted",
                        "Lead converted to Project: " + savedProject.getName(), savedProject, convertedBy);
            } catch (Exception e) {
                logger.warn("Failed to log conversion activity: {}", e.getMessage());
            }

            return savedProject;
        } catch (Exception e) {
            logger.error("CRITICAL ERROR in lead conversion for lead {}: {}", leadId, e.getMessage(), e);
            throw e;
        }
    }

    private void migrateQuotationToBoq(Long quoteId, com.wd.api.model.CustomerProject project) {
        com.wd.api.model.LeadQuotation quote = leadQuotationRepository.findById(quoteId).orElse(null);
        if (quote != null && !quote.getItems().isEmpty()) {
            com.wd.api.model.BoqWorkType defaultWorkType = boqWorkTypeRepository.findByName("General Works")
                    .orElseGet(() -> {
                        com.wd.api.model.BoqWorkType wt = new com.wd.api.model.BoqWorkType();
                        wt.setName("General Works");
                        wt.setDisplayOrder(1);
                        return boqWorkTypeRepository.save(wt);
                    });

            for (com.wd.api.model.LeadQuotationItem quoteItem : quote.getItems()) {
                com.wd.api.model.BoqItem boqItem = new com.wd.api.model.BoqItem();
                boqItem.setProject(project);
                boqItem.setWorkType(defaultWorkType);
                boqItem.setDescription(quoteItem.getDescription());
                boqItem.setQuantity(quoteItem.getQuantity());
                boqItem.setUnitRate(quoteItem.getUnitPrice());
                boqItem.setTotalAmount(quoteItem.getTotalPrice());
                boqItem.setUnit("LS");
                boqItemRepository.save(boqItem);
            }
        }
    }

    private com.wd.api.model.CustomerUser createCustomerFromLead(Lead lead) {
        com.wd.api.model.CustomerUser customer = new com.wd.api.model.CustomerUser();
        customer.setEmail(lead.getEmail());

        String fullName = lead.getName().trim();
        int lastSpaceIndex = fullName.lastIndexOf(" ");
        if (lastSpaceIndex != -1) {
            customer.setFirstName(fullName.substring(0, lastSpaceIndex));
            customer.setLastName(fullName.substring(lastSpaceIndex + 1));
        } else {
            customer.setFirstName(fullName);
            customer.setLastName(".");
        }

        customer.setPhone(lead.getPhone());
        customer.setWhatsappNumber(lead.getWhatsappNumber());
        customer.setAddress(lead.getAddress());
        customer.setLeadSource(lead.getLeadSource());
        customer.setNotes(lead.getNotes());
        customer.setEnabled(true);

        String tempPassword = generateSecurePassword();
        customer.setPassword(passwordEncoder.encode(tempPassword));

        try {
            emailService.sendWelcomeEmail(customer.getEmail(), customer.getFirstName() + " " + customer.getLastName(),
                    tempPassword);
        } catch (Exception e) {
            logger.warn("Failed to send welcome email to {}: {}", customer.getEmail(), e.getMessage());
        }

        customerRoleRepository.findByName("CUSTOMER").ifPresent(customer::setRole);

        return customerUserRepository.save(customer);
    }

    /**
     * Calculate lead score based on multiple factors
     * Scoring factors:
     * - Budget: High (>50L) = 25, Medium (>10L) = 15, Low = 5
     * - Lead Source: Referral = 20, Website/Organic = 10, Others = 5
     * - Priority: High = 15, Medium = 10, Low = 5
     * - Probability to Win: >70% = 15, 40-70% = 10, <40% = 5
     * - Client Rating: 4-5 stars = 10, 3 stars = 5, <3 = 0
     * - Project Area: >3000 sqft = 10, 1500-3000 = 5, <1500 = 0
     * 
     * Score Categories:
     * - HOT: >60 points
     * - WARM: 30-60 points
     * - COLD: <30 points
     */
    private void calculateLeadScore(Lead lead) {
        int score = 0;
        Map<String, Integer> factors = new HashMap<>();
        try {
            // Budget scoring
            if (lead.getBudget() != null) {
                if (lead.getBudget().compareTo(new BigDecimal("5000000")) > 0) {
                    score += 25;
                    factors.put("High Budget (>50L)", 25);
                } else if (lead.getBudget().compareTo(new BigDecimal("1000000")) > 0) {
                    score += 15;
                    factors.put("Medium Budget (10L-50L)", 15);
                } else {
                    score += 5;
                    factors.put("Low Budget (<10L)", 5);
                }
            }

            // Lead Source scoring
            if (lead.getLeadSource() != null) {
                String source = lead.getLeadSource().toLowerCase();
                if (source.contains("referral")) {
                    score += 20;
                    factors.put("Referral Source", 20);
                } else if (source.contains("website") || source.contains("organic")) {
                    score += 10;
                    factors.put("Organic Source", 10);
                } else {
                    score += 5;
                    factors.put("Other Source", 5);
                }
            }

            // Priority scoring
            if (lead.getPriority() != null) {
                String priority = lead.getPriority().toLowerCase();
                if ("high".equals(priority)) {
                    score += 15;
                    factors.put("High Priority", 15);
                } else if ("medium".equals(priority)) {
                    score += 10;
                    factors.put("Medium Priority", 10);
                } else {
                    score += 5;
                    factors.put("Low Priority", 5);
                }
            }

            // Probability to Win scoring
            if (lead.getProbabilityToWin() != null) {
                int prob = lead.getProbabilityToWin();
                if (prob > 70) {
                    score += 15;
                    factors.put("High Probability (>70%)", 15);
                } else if (prob >= 40) {
                    score += 10;
                    factors.put("Medium Probability (40-70%)", 10);
                } else {
                    score += 5;
                    factors.put("Low Probability (<40%)", 5);
                }
            }

            // Client Rating scoring
            if (lead.getClientRating() != null) {
                int rating = lead.getClientRating();
                if (rating >= 4) {
                    score += 10;
                    factors.put("High Rating (4-5 stars)", 10);
                } else if (rating == 3) {
                    score += 5;
                    factors.put("Medium Rating (3 stars)", 5);
                }
            }

            // Project Area scoring
            if (lead.getProjectSqftArea() != null) {
                BigDecimal area = lead.getProjectSqftArea();
                if (area.compareTo(new BigDecimal("3000")) > 0) {
                    score += 10;
                    factors.put("Large Project (>3000 sqft)", 10);
                } else if (area.compareTo(new BigDecimal("1500")) > 0) {
                    score += 5;
                    factors.put("Medium Project (1500-3000 sqft)", 5);
                }
            }

            // Interaction Frequency scoring (0-20 points)
            if (lead.getId() != null) {
                try {
                    long interactionCount = leadInteractionRepository
                            .findByLeadIdOrderByInteractionDateDesc(lead.getId()).size();
                    if (interactionCount >= 5) {
                        score += 20;
                        factors.put("High Engagement (5+ interactions)", 20);
                    } else if (interactionCount >= 3) {
                        score += 15;
                        factors.put("Medium Engagement (3-4 interactions)", 15);
                    } else if (interactionCount >= 1) {
                        score += 10;
                        factors.put("Initial Contact (1-2 interactions)", 10);
                    }
                } catch (Exception e) {
                    logger.warn("Error fetching interaction count for lead {}: {}", lead.getId(), e.getMessage());
                    // Continue scoring without interaction count - don't break the update
                }
            }

            // Timeline Urgency scoring (0-15 points)
            if (lead.getNextFollowUp() != null) {
                try {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime followUpDate = lead.getNextFollowUp();
                    // Ensure followUpDate is not null (double check)
                    if (followUpDate != null) {
                        long daysUntilFollowUp = java.time.Duration.between(now, followUpDate).toDays();
                        if (daysUntilFollowUp < 0) {
                            // Overdue follow-up - high urgency
                            score += 15;
                            factors.put("Overdue Follow-up (Urgent)", 15);
                        } else if (daysUntilFollowUp <= 3) {
                            // Follow-up within 3 days - medium-high urgency
                            score += 12;
                            factors.put("Immediate Follow-up (0-3 days)", 12);
                        } else if (daysUntilFollowUp <= 7) {
                            // Follow-up within a week - medium urgency
                            score += 8;
                            factors.put("Near-term Follow-up (4-7 days)", 8);
                        } else if (daysUntilFollowUp <= 30) {
                            // Follow-up within a month - low-medium urgency
                            score += 5;
                            factors.put("Scheduled Follow-up (8-30 days)", 5);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error calculating timeline urgency for lead {}: {}", lead.getId(), e.getMessage());
                    // Continue scoring without timeline urgency - don't break the update
                }
            }

            // Location scoring (0-10 points) - Premium locations get higher scores
            if (lead.getState() != null) {
                String state = lead.getState().toLowerCase();
                // Tier 1 cities/states (e.g., Karnataka, Maharashtra, Tamil Nadu, Delhi)
                if (state.contains("karnataka") || state.contains("maharashtra") ||
                        state.contains("tamil") || state.contains("delhi") ||
                        state.contains("gujarat") || state.contains("telangana")) {
                    score += 10;
                    factors.put("Premium Location (Tier 1)", 10);
                } else if (state.contains("kerala") || state.contains("punjab") ||
                        state.contains("haryana") || state.contains("rajasthan")) {
                    score += 7;
                    factors.put("Good Location (Tier 2)", 7);
                } else {
                    score += 5;
                    factors.put("Standard Location", 5);
                }
            }

            // Contact Completeness scoring (0-10 points)
            int contactCompleteness = 0;
            if (lead.getEmail() != null && !lead.getEmail().trim().isEmpty())
                contactCompleteness += 3;
            if (lead.getPhone() != null && !lead.getPhone().trim().isEmpty())
                contactCompleteness += 3;
            if (lead.getWhatsappNumber() != null && !lead.getWhatsappNumber().trim().isEmpty())
                contactCompleteness += 2;
            if (lead.getAddress() != null && !lead.getAddress().trim().isEmpty())
                contactCompleteness += 2;
            if (contactCompleteness > 0) {
                score += contactCompleteness;
                factors.put("Contact Completeness", contactCompleteness);
            }

            // Ensure score is within bounds (0-100)
            score = Math.min(100, Math.max(0, score));

            lead.setScore(score);
            lead.setScoreCategory(score > 60 ? "HOT" : (score >= 30 ? "WARM" : "COLD"));
            lead.setLastScoredAt(LocalDateTime.now());
            try {
                // Use thread-safe ObjectMapper instance
                lead.setScoreFactors(objectMapper.writeValueAsString(factors));
            } catch (Exception e) {
                logger.warn("Error serializing score factors for lead {}: {}", lead.getId(), e.getMessage());
                lead.setScoreFactors("{}");
            }
        } catch (Exception e) {
            logger.warn("Error calculating lead score: {}", e.getMessage());
            lead.setScore(0);
            lead.setScoreCategory("COLD");
            lead.setScoreFactors("{}");
        }
    }

    /**
     * Quick status update endpoint - updates only the status field
     * Enterprise-grade with status transition validation
     */
    @Transactional
    public Lead updateLeadStatus(Long id, String newStatus) {
        if (id == null) {
            throw new IllegalArgumentException("Lead ID cannot be null");
        }
        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }

        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + id));

        String oldStatus = lead.getLeadStatus();

        // Validate status transition
        if (!newStatus.equals(oldStatus)) {
            validateLeadStatusTransition(oldStatus, newStatus);
        }

        lead.setLeadStatus(newStatus);
        Lead savedLead = leadRepository.save(lead);

        // Log activity
        try {
            if (!newStatus.equals(oldStatus)) {
                activityFeedService.logSystemActivity(
                        "LEAD_STATUS_CHANGED",
                        "Lead Status Changed",
                        "Status changed from " + oldStatus + " to " + newStatus,
                        savedLead.getId(),
                        "LEAD");
            }
        } catch (Exception e) {
            logger.warn("Error logging status change activity: {}", e.getMessage());
        }

        return savedLead;
    }

    /**
     * Quick assignment update endpoint - updates only the assignedTo field
     */
    @Transactional
    public Lead assignLead(Long id, Long assignedToId) {
        if (id == null) {
            throw new IllegalArgumentException("Lead ID cannot be null");
        }

        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + id));

        Long oldAssignedId = lead.getAssignedTo() != null ? lead.getAssignedTo().getId() : null;

        if (assignedToId != null) {
            com.wd.api.model.PortalUser user = portalUserRepository.findById(assignedToId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + assignedToId));
            lead.setAssignedTo(user);
            lead.setAssignedTeam(user.getFirstName() + " " + user.getLastName());
        } else {
            lead.setAssignedTo(null);
            lead.setAssignedTeam(null);
        }

        Lead savedLead = leadRepository.save(lead);

        // Log activity
        try {
            String oldAssignee = oldAssignedId != null ? "User " + oldAssignedId : "Unassigned";
            String newAssignee = assignedToId != null ? savedLead.getAssignedTeam() : "Unassigned";

            if (!oldAssignee.equals(newAssignee)) {
                activityFeedService.logSystemActivity(
                        "LEAD_ASSIGNED",
                        "Lead Assigned",
                        "Lead assigned from " + oldAssignee + " to " + newAssignee,
                        savedLead.getId(),
                        "LEAD");
            }
        } catch (Exception e) {
            logger.warn("Error logging assignment activity: {}", e.getMessage());
        }

        return savedLead;
    }

    /**
     * Manual score update endpoint - updates score and score category
     * Note: This bypasses automatic score calculation
     */
    @Transactional
    public Lead updateLeadScore(Long id, Integer score, String reason) {
        if (id == null) {
            throw new IllegalArgumentException("Lead ID cannot be null");
        }
        if (score == null || score < 0 || score > 100) {
            throw new IllegalArgumentException("Score must be between 0 and 100");
        }

        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + id));

        Integer oldScore = lead.getScore();
        String oldCategory = lead.getScoreCategory();

        // Update score
        lead.setScore(score);

        // Determine category based on score
        String category;
        if (score >= 80) {
            category = "HOT";
        } else if (score >= 50) {
            category = "WARM";
        } else {
            category = "COLD";
        }
        lead.setScoreCategory(category);

        Lead savedLead = leadRepository.save(lead);

        // Log score history
        try {
            leadScoreHistoryService.logScoreChange(
                    savedLead,
                    oldScore,
                    oldCategory,
                    savedLead.getUpdatedByUserId(),
                    reason != null ? reason : "Manual score update",
                    savedLead.getScoreFactors());
        } catch (Exception e) {
            logger.warn("Error logging score change: {}", e.getMessage());
        }

        // Log activity
        try {
            activityFeedService.logSystemActivity(
                    "LEAD_SCORE_UPDATED",
                    "Lead Score Updated",
                    "Score changed from " + oldScore + " to " + score + " (Category: " + category + ")",
                    savedLead.getId(),
                    "LEAD");
        } catch (Exception e) {
            logger.warn("Error logging score update activity: {}", e.getMessage());
        }

        return savedLead;
    }

    /**
     * Get conversion history for a lead
     * Returns list of projects that were converted from this lead (if any)
     */
    public List<Map<String, Object>> getConversionHistory(Long leadId) {
        if (leadId == null) {
            throw new IllegalArgumentException("Lead ID cannot be null");
        }

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));

        List<Map<String, Object>> history = new ArrayList<>();

        // Check if lead has been converted
        if (lead.getConvertedAt() != null && lead.getConvertedById() != null) {
            // Find the project that was converted from this lead
            com.wd.api.model.CustomerProject project = customerProjectRepository
                    .findByLeadId(leadId)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (project != null) {
                Map<String, Object> conversion = new HashMap<>();
                conversion.put("projectId", project.getId());
                conversion.put("projectName", project.getName());
                conversion.put("convertedAt", lead.getConvertedAt());
                conversion.put("convertedById", lead.getConvertedById());

                // Get converter user name
                portalUserRepository.findById(lead.getConvertedById()).ifPresent(user -> {
                    conversion.put("convertedByName", user.getFirstName() + " " + user.getLastName());
                });

                conversion.put("status", "SUCCESS");
                history.add(conversion);
            }
        }

        return history;
    }

    private String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "@1";
    }
}
