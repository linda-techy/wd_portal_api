package com.wd.api.service;

import com.wd.api.model.Lead;
import com.wd.api.dto.PaginationParams;
import com.wd.api.dto.PartnershipReferralRequest;
import com.wd.api.repository.LeadRepository;
import com.wd.api.repository.LeadQuotationRepository;
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

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LeadService.class);

    @Autowired
    private com.wd.api.repository.ProjectMemberRepository projectMemberRepository;

    @Autowired
    private com.wd.api.repository.BoqWorkTypeRepository boqWorkTypeRepository;

    @Autowired
    private com.wd.api.repository.BoqItemRepository boqItemRepository;

    @Transactional
    public Lead createLead(Lead lead) {
        if (lead.getDateOfEnquiry() == null) {
            lead.setDateOfEnquiry(LocalDate.now());
        }
        if (lead.getCreatedAt() == null) {
            lead.setCreatedAt(java.time.LocalDateTime.now());
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
            e.printStackTrace();
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

    @Transactional
    public Lead updateLead(Long id, Lead leadDetails) {
        if (id == null)
            return null;
        return leadRepository.findById(id).map(lead -> {
            String oldStatus = lead.getLeadStatus();
            String oldCategory = lead.getScoreCategory(); // Capture old score category
            String oldAssigned = lead.getAssignedTeam();
            Long oldAssignedId = lead.getAssignedTo() != null ? lead.getAssignedTo().getId() : null;

            lead.setName(leadDetails.getName());
            lead.setEmail(leadDetails.getEmail());
            lead.setPhone(leadDetails.getPhone());
            lead.setWhatsappNumber(leadDetails.getWhatsappNumber());
            lead.setLeadSource(leadDetails.getLeadSource());
            lead.setLeadStatus(leadDetails.getLeadStatus());
            lead.setPriority(leadDetails.getPriority());
            lead.setCustomerType(leadDetails.getCustomerType());
            lead.setProjectType(leadDetails.getProjectType());
            lead.setProjectDescription(leadDetails.getProjectDescription());
            lead.setRequirements(leadDetails.getRequirements());
            lead.setBudget(leadDetails.getBudget());
            lead.setProjectSqftArea(leadDetails.getProjectSqftArea());
            lead.setNextFollowUp(leadDetails.getNextFollowUp());
            lead.setLastContactDate(leadDetails.getLastContactDate());
            lead.setAssignedTeam(leadDetails.getAssignedTeam());
            lead.setState(leadDetails.getState());
            lead.setDistrict(leadDetails.getDistrict());
            lead.setLocation(leadDetails.getLocation());
            lead.setAddress(leadDetails.getAddress());
            lead.setNotes(leadDetails.getNotes());
            lead.setClientRating(leadDetails.getClientRating());
            lead.setProbabilityToWin(leadDetails.getProbabilityToWin());
            lead.setLostReason(leadDetails.getLostReason());
            lead.setDateOfEnquiry(leadDetails.getDateOfEnquiry());

            // Handle Assignment Update
            if (leadDetails.getAssignedToId() != null) {
                if (!leadDetails.getAssignedToId().equals(oldAssignedId)) {
                    Long assignedId = java.util.Objects.requireNonNull(leadDetails.getAssignedToId());
                    com.wd.api.model.PortalUser user = portalUserRepository.findById(assignedId)
                            .orElse(null);
                    if (user != null) {
                        lead.setAssignedTo(user);
                        lead.setAssignedToId(leadDetails.getAssignedToId());
                        lead.setAssignedTeam(user.getFirstName() + " " + user.getLastName());
                    }
                }
            }

            // Recalculate Score on Update
            calculateLeadScore(lead);

            lead.setUpdatedAt(java.time.LocalDateTime.now());

            Lead savedLead = leadRepository.save(lead);

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
        }).orElse(null);
    }

    @Transactional
    public boolean deleteLead(Long id) {
        if (id != null && leadRepository.existsById(id)) {
            leadRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<com.wd.api.model.ActivityFeed> getLeadActivities(Long leadId) {
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

    public Page<Lead> getLeadsPaginated(PaginationParams params) {
        String sortOrder = params.getSortOrder() != null ? params.getSortOrder() : "DESC";
        String sortBy = params.getSortBy() != null ? params.getSortBy() : "createdAt";
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), mapSortField(sortBy));
        int pageZeroBased = Math.max(0, params.getPage() - 1);
        Pageable pageable = PageRequest.of(pageZeroBased, params.getLimit(), sort);

        Specification<Lead> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (params.getStatus() != null && !params.getStatus().isEmpty()) {
                predicates.add(cb.equal(root.get("leadStatus"), params.getStatus()));
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

    private void calculateLeadScore(Lead lead) {
        int score = 0;
        Map<String, Integer> factors = new HashMap<>();
        try {
            if (lead.getBudget() != null) {
                if (lead.getBudget().compareTo(new BigDecimal("5000000")) > 0) {
                    score += 20;
                    factors.put("High Budget", 20);
                } else if (lead.getBudget().compareTo(new BigDecimal("1000000")) > 0) {
                    score += 10;
                    factors.put("Medium Budget", 10);
                }
            }
            if (lead.getLeadSource() != null) {
                String source = lead.getLeadSource().toLowerCase();
                if (source.contains("referral")) {
                    score += 20;
                    factors.put("Referral", 20);
                } else if (source.contains("website")) {
                    score += 10;
                    factors.put("Organic", 10);
                }
            }

            lead.setScore(score);
            lead.setScoreCategory(score > 60 ? "HOT" : (score >= 30 ? "WARM" : "COLD"));
            lead.setLastScoredAt(LocalDateTime.now());
            try {
                lead.setScoreFactors(new ObjectMapper().writeValueAsString(factors));
            } catch (Exception e) {
                lead.setScoreFactors("{}");
            }
        } catch (Exception e) {
            lead.setScore(0);
            lead.setScoreCategory("COLD");
        }
    }

    private String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "@1";
    }
}
