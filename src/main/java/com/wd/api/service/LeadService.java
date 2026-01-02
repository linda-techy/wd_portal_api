package com.wd.api.service;

import com.wd.api.dao.model.Leads;
import com.wd.api.dto.PaginationParams;
import com.wd.api.dto.PartnershipReferralRequest;
import com.wd.api.repository.LeadsRepository;
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
    private LeadsRepository leadsRepository;

    @Autowired
    private ActivityFeedService activityFeedService;

    @Autowired
    private com.wd.api.repository.PortalUserRepository portalUserRepository;

    @Autowired
    private com.wd.api.repository.CustomerUserRepository customerUserRepository;

    @Autowired
    private com.wd.api.repository.CustomerProjectRepository customerProjectRepository;

    @Autowired
    private com.wd.api.repository.UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private LeadQuotationRepository leadQuotationRepository;

    @Autowired
    private com.wd.api.repository.BoqItemRepository boqItemRepository;

    @Autowired
    private com.wd.api.repository.BoqWorkTypeRepository boqWorkTypeRepository;

    @Autowired
    private com.wd.api.repository.ProjectMemberRepository projectMemberRepository;

    @Transactional
    public Leads createLead(Leads lead) {
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

        Leads savedLead = leadsRepository.save(lead);
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
    public Leads createLead(LeadCreateRequest request) {
        Leads lead = new Leads();
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
    public Leads updateLead(Long id, Leads leadDetails) {
        if (id == null)
            return null;
        return leadsRepository.findById(id).map(lead -> {
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
                    com.wd.api.model.PortalUser user = portalUserRepository.findById(leadDetails.getAssignedToId())
                            .orElse(null);
                    if (user != null) {
                        lead.setAssignedTo(user);
                        lead.setAssignedToId(leadDetails.getAssignedToId());
                        lead.setAssignedTeam(user.getFirstName() + " " + user.getLastName());
                    }
                }
            } else if (leadDetails.getAssignedToId() == null && oldAssignedId != null) {
                // Determine if we should clear it?
                // Currently Request might send null if not changing.
                // But DTO usually sends whole object.
                // Let's assume if sent as 0 or explicit null action needed?
                // For now, if passed ID is distinct (and valid), we update.
            }

            // Recalculate Score on Update
            calculateLeadScore(lead);

            lead.setUpdatedAt(java.time.LocalDateTime.now());

            Leads savedLead = leadsRepository.save(lead);

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

                // Logic updated to check assignedTo relationship change or fallback string
                // change
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
        if (id != null && leadsRepository.existsById(id)) {
            leadsRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<com.wd.api.model.ActivityFeed> getLeadActivities(Long leadId) {
        return activityFeedService.getActivitiesForLead(leadId);
    }

    public Leads getLeadById(Long id) {
        if (id == null)
            return null;
        return leadsRepository.findById(id).orElse(null);
    }

    public List<Leads> getAllLeads() {
        return leadsRepository.findAll();
    }

    public Page<Leads> getLeadsPaginated(PaginationParams params) {
        Sort sort = Sort.by(Sort.Direction.fromString(params.getSortOrder()), mapSortField(params.getSortBy()));
        int pageZeroBased = Math.max(0, params.getPage() - 1);
        Pageable pageable = PageRequest.of(pageZeroBased, params.getLimit(), sort);

        Specification<Leads> spec = (root, query, cb) -> {
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

            if (params.getSearch() != null && !params.getSearch().isEmpty()) {
                String likePattern = "%" + params.getSearch().toLowerCase() + "%";
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

        return leadsRepository.findAll(spec, pageable);
    }

    // Analytics - OPTIMIZED (JPA Aggregations)
    public Map<String, Object> getLeadAnalytics() {
        Map<String, Object> analytics = new java.util.HashMap<>();

        // Status distribution using JPA count
        Map<String, Long> statusDist = new java.util.HashMap<>();
        List<String> statuses = List.of("new", "contacted", "qualified", "proposal_sent", "won", "lost");
        for (String status : statuses) {
            statusDist.put(status, leadsRepository.countByLeadStatus(status));
        }
        analytics.put("statusDistribution", statusDist);

        // Source distribution - Optimized
        List<Object[]> sourceCounts = leadsRepository.countLeadsBySource();
        Map<String, Long> sourceDist = new HashMap<>();
        for (Object[] row : sourceCounts) {
            String source = (String) row[0];
            Long count = (Long) row[1];
            sourceDist.put(source != null ? source : "unknown", count);
        }
        analytics.put("sourceDistribution", sourceDist);

        // Priority distribution - Optimized
        List<Object[]> priorityCounts = leadsRepository.countLeadsByPriority();
        Map<String, Long> priorityDist = new HashMap<>();
        for (Object[] row : priorityCounts) {
            String priority = (String) row[0];
            Long count = (Long) row[1];
            priorityDist.put(priority != null ? priority : "unknown", count);
        }
        analytics.put("priorityDistribution", priorityDist);

        // Monthly trends - simplified
        analytics.put("monthlyTrends", "Use custom @Query for monthly trend analysis");

        return analytics;
    }

    public Map<String, Object> getLeadConversionMetrics() {
        Map<String, Object> metrics = new java.util.HashMap<>();

        // Total leads using JPA repository count
        long totalLeads = leadsRepository.count();
        metrics.put("totalLeads", totalLeads);

        // Converted/Won leads (changed from 'converted' to 'won' to match actual
        // status)
        long convertedLeads = leadsRepository.countByLeadStatus("won");
        metrics.put("convertedLeads", convertedLeads);

        // Calculate conversion rate
        if (totalLeads > 0) {
            double conversionRate = (convertedLeads * 100.0) / totalLeads;
            metrics.put("conversionRate", Math.round(conversionRate * 100.0) / 100.0);
        } else {
            metrics.put("conversionRate", 0.0);
        }

        return metrics;
    }

    public List<Leads> getOverdueFollowUps() {
        // Use existing repository method instead of JDBC
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<String> excludedStatuses = List.of("won", "lost");
        return leadsRepository.findByNextFollowUpBeforeAndLeadStatusNotIn(now, excludedStatuses);
    }

    public List<Leads> getLeadsByStatus(String status) {
        return leadsRepository.findByLeadStatus(status);
    }

    public List<Leads> getLeadsBySource(String source) {
        return leadsRepository.findByLeadSource(source);
    }

    public List<Leads> getLeadsByPriority(String priority) {
        return leadsRepository.findByPriority(priority);
    }

    public List<Leads> getLeadsByAssignedTo(String teamIdLike) {
        try {
            Long id = Long.parseLong(teamIdLike);
            return leadsRepository.findByAssignedTo_Id(id);
        } catch (NumberFormatException e) {
            return leadsRepository.findByAssignedTeam(teamIdLike);
        }
    }

    public List<Leads> searchLeads(String query) {
        return leadsRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCase(
                query, query, query);
    }

    public Leads createLeadFromPartnershipReferral(PartnershipReferralRequest request) {
        Leads lead = new Leads();
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

        return leadsRepository.save(lead);
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
                return dbColumn; // Assume matches property name (e.g., name, email, priority)
        }
    }

    /**
     * Enterprise One-Click Lead Conversion
     * Converts a simplified Lead into a full-fledged Customer Project
     * 1. Creates/Links Customer User account
     * 2. Creates Customer Project
     * 3. Links Project to Lead
     * 4. Updates Lead Status to WON
     * 5. Logs Audit Activity
     */
    @Transactional
    public com.wd.api.model.CustomerProject convertLead(Long leadId, com.wd.api.dto.LeadConversionRequest request,
            String username) {
        if (leadId == null)
            throw new IllegalArgumentException("Lead ID cannot be null");
        Leads lead = leadsRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));

        com.wd.api.model.User convertedBy = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + username));

        // Validate Status
        if ("WON".equalsIgnoreCase(lead.getLeadStatus()) || "converted".equalsIgnoreCase(lead.getLeadStatus())) {
            throw new IllegalStateException("Lead is already converted to a project");
        }
        if ("LOST".equalsIgnoreCase(lead.getLeadStatus()) || "lost".equalsIgnoreCase(lead.getLeadStatus())) {
            throw new IllegalArgumentException("Cannot convert a lost lead. Please update lead status first.");
        }

        // Check for duplicate conversion
        if (leadId != null && customerProjectRepository.existsByLeadId(leadId)) {
            throw new IllegalStateException("This lead has already been converted to a project");
        }

        // 1. Create or Find Customer User
        com.wd.api.model.CustomerUser customer = customerUserRepository.findByEmail(lead.getEmail())
                .orElseGet(() -> createCustomerFromLead(lead));

        // 2. Create Project
        com.wd.api.model.CustomerProject project = new com.wd.api.model.CustomerProject();
        project.setName(request.getProjectName() != null ? request.getProjectName() : lead.getName() + " Project");
        project.setCustomer(customer);
        project.setLeadId(lead.getId()); // Link back to lead
        project.setStartDate(request.getStartDate() != null ? request.getStartDate() : java.time.LocalDate.now());
        project.setProjectPhase(com.wd.api.model.enums.ProjectPhase.PLANNING); // Default phase
        project.setState("Active");
        project.setLocation(request.getLocation() != null ? request.getLocation() : lead.getLocation());
        project.setSqfeet(lead.getProjectSqftArea());
        project.setProjectType(request.getProjectType());

        // Construction details
        project.setPlotArea(lead.getPlotArea());
        project.setFloors(lead.getFloors());
        project.setProjectDescription(lead.getProjectDescription());

        // Assign Project Manager if selected
        if (request.getProjectManagerId() != null) {
            project.setProjectManagerId(request.getProjectManagerId());
        }
        project.setCreatedBy(convertedBy.getUsername());

        // Set Budget from Quote or Lead
        // Set Budget from Quote or Lead
        if (request.getQuotationId() != null) {
            com.wd.api.model.LeadQuotation quote = leadQuotationRepository.findById(request.getQuotationId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Quotation not found: " + request.getQuotationId()));

            if (!quote.getLeadId().equals(lead.getId())) {
                throw new IllegalArgumentException("Quotation does not belong to this lead");
            }

            project.setBudget(quote.getFinalAmount());
            project.setDesignPackage(quote.getTitle());

            // Mark Quote as Accepted
            quote.setStatus("ACCEPTED");
            quote.setRespondedAt(java.time.LocalDateTime.now());
            leadQuotationRepository.save(quote);
        } else {
            project.setBudget(lead.getBudget());
        }

        // Save Project First to get ID
        com.wd.api.model.CustomerProject savedProject = customerProjectRepository.save(project);

        // Generate Enterprise Project Code
        String projectCode = "PRJ-" + java.time.LocalDate.now().getYear() + "-"
                + String.format("%04d", savedProject.getId());
        savedProject.setCode(projectCode);

        // Update Metadata
        savedProject.setConvertedById(convertedBy.getId());
        savedProject.setConvertedFromLeadId(lead.getId());
        savedProject.setConvertedAt(java.time.LocalDateTime.now());

        savedProject = customerProjectRepository.save(savedProject);

        // 4. Assign Project Manager
        if (request.getProjectManagerId() != null) {
            Long pmId = request.getProjectManagerId();
            // Set ID on Project Entity
            savedProject.setProjectManagerId(pmId);
            savedProject = customerProjectRepository.save(savedProject);

            if (pmId != null) {
                com.wd.api.model.PortalUser pmUser = portalUserRepository.findById(pmId)
                        .orElse(null);
                if (pmUser != null) {
                    com.wd.api.model.ProjectMember pmMember = new com.wd.api.model.ProjectMember();
                    pmMember.setProject(savedProject);
                    pmMember.setPortalUser(pmUser);
                    pmMember.setRoleInProject("PROJECT_MANAGER");
                    projectMemberRepository.save(pmMember);
                }
            }
        }

        // Migrate Quotation Items to BoQ
        if (request.getQuotationId() != null) {
            Long quoteId = request.getQuotationId();
            if (quoteId != null) {
                com.wd.api.model.LeadQuotation quote = leadQuotationRepository.findById(quoteId)
                        .orElse(null);
                if (quote != null && !quote.getItems().isEmpty()) {
                    com.wd.api.model.BoqWorkType defaultWorkType = boqWorkTypeRepository.findByName("General Works")
                            .orElseGet(() -> {
                                com.wd.api.model.BoqWorkType wt = new com.wd.api.model.BoqWorkType();
                                wt.setName("General Works");
                                wt.setDescription("General construction items from quotation");
                                wt.setDisplayOrder(1);
                                return boqWorkTypeRepository.save(wt);
                            });

                    for (com.wd.api.model.LeadQuotationItem quoteItem : quote.getItems()) {
                        com.wd.api.model.BoqItem boqItem = new com.wd.api.model.BoqItem();
                        boqItem.setProject(savedProject);
                        boqItem.setWorkType(defaultWorkType);
                        boqItem.setDescription(quoteItem.getDescription());
                        boqItem.setQuantity(quoteItem.getQuantity());
                        boqItem.setUnitRate(quoteItem.getUnitPrice());
                        boqItem.setTotalAmount(quoteItem.getTotalPrice());
                        boqItem.setUnit("LS");
                        boqItem.setNotes("Imported from Quote #" + quote.getQuotationNumber());

                        boqItemRepository.save(boqItem);
                    }
                }
            }
        }

        // Update Lead Status
        lead.setLeadStatus("WON");
        leadsRepository.save(lead);

        // Log Activity
        try {
            activityFeedService.logSystemActivity(
                    "LEAD_CONVERTED",
                    "Lead Converted",
                    "Lead converted to Project: " + savedProject.getName(),
                    lead.getId(),
                    "LEAD");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return savedProject;
    }

    private com.wd.api.model.CustomerUser createCustomerFromLead(Leads lead) {
        com.wd.api.model.CustomerUser customer = new com.wd.api.model.CustomerUser();
        customer.setEmail(lead.getEmail());

        // Proper name splitting logic
        String fullName = lead.getName().trim();
        int lastSpaceIndex = fullName.lastIndexOf(" ");
        if (lastSpaceIndex != -1) {
            customer.setFirstName(fullName.substring(0, lastSpaceIndex));
            customer.setLastName(fullName.substring(lastSpaceIndex + 1));
        } else {
            customer.setFirstName(fullName);
            customer.setLastName("."); // Placeholder for last name as it isn't optional
        }

        // Map business fields from Lead to CustomerUser (Phase 1.2 Data Integrity)
        customer.setPhone(lead.getPhone());
        customer.setWhatsappNumber(lead.getWhatsappNumber());
        customer.setAddress(lead.getAddress());
        customer.setLeadSource(lead.getLeadSource());
        customer.setNotes(lead.getNotes()); // Transfer notes history

        customer.setEnabled(true);

        // Generate secure random password instead of hardcoded one
        String tempPassword = generateSecurePassword();
        customer.setPassword(passwordEncoder.encode(tempPassword));

        // Send Welcome Email (Async/Simulated)
        emailService.sendWelcomeEmail(customer.getEmail(), customer.getFirstName() + " " + customer.getLastName(),
                tempPassword);

        return customerUserRepository.save(customer);
    }

    private void calculateLeadScore(Leads lead) {
        int score = 0;
        Map<String, Integer> factors = new HashMap<>();

        try {
            // Budget
            if (lead.getBudget() != null) {
                if (lead.getBudget().compareTo(new BigDecimal("5000000")) > 0) {
                    score += 20;
                    factors.put("High Budget (>5M)", 20);
                } else if (lead.getBudget().compareTo(new BigDecimal("1000000")) > 0) {
                    score += 10;
                    factors.put("Medium Budget (>1M)", 10);
                }
            }

            // Source
            if (lead.getLeadSource() != null) {
                String source = lead.getLeadSource().toLowerCase();
                if (source.contains("referral")) {
                    score += 20;
                    factors.put("Referral Interest", 20);
                } else if (source.contains("website") || source.contains("google")) {
                    score += 10;
                    factors.put("Organic Interest", 10);
                } else {
                    score += 5;
                    factors.put("Standard Entry", 5);
                }
            }

            // Contact Info Integrity
            if (lead.getEmail() != null && !lead.getEmail().isEmpty() &&
                    lead.getPhone() != null && !lead.getPhone().isEmpty() &&
                    lead.getWhatsappNumber() != null && !lead.getWhatsappNumber().isEmpty()) {
                score += 10;
                factors.put("Complete Contact Profile", 10);
            }

            // Project Type Priority
            if (lead.getProjectType() != null) {
                String type = lead.getProjectType().toLowerCase();
                if (type.contains("commercial") || type.contains("industrial")) {
                    score += 15;
                    factors.put("Commercial Value", 15);
                } else if (type.contains("residential")) {
                    score += 10;
                    factors.put("Residential Request", 10);
                }
            }

            // Status-based Progression Bonus
            if (lead.getLeadStatus() != null &&
                    ("qualified_lead".equalsIgnoreCase(lead.getLeadStatus())
                            || "qualified".equalsIgnoreCase(lead.getLeadStatus()))) {
                score += 5;
                factors.put("Qualified Status", 5);
            }

            // Determine Category
            String category;
            if (score > 60)
                category = "HOT";
            else if (score >= 30)
                category = "WARM";
            else
                category = "COLD";

            lead.setScore(score);
            lead.setScoreCategory(category);
            lead.setLastScoredAt(LocalDateTime.now());

            // Serialize factors
            try {
                lead.setScoreFactors(new ObjectMapper().writeValueAsString(factors));
            } catch (Exception e) {
                lead.setScoreFactors("{}");
            }
        } catch (Exception e) {
            // Failsafe: Don't block lead creation/update if scoring fails
            e.printStackTrace();
            lead.setScore(0);
            lead.setScoreCategory("COLD");
        }
    }

    /**
     * Generate a secure random password
     * Format: 12 chars, alphanumeric + special chars
     */
    private String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        // Base64Url ensures alphanumeric. Append special char to satisfy strict policy
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "@1";
    }
}
