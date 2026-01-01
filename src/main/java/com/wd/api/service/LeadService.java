package com.wd.api.service;

import com.wd.api.dao.model.Leads;
import com.wd.api.dto.PaginationParams;
import com.wd.api.dto.PartnershipReferralRequest;
import com.wd.api.repository.LeadsRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LeadService {

    @Autowired
    private LeadsRepository leadsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ActivityFeedService activityFeedService;

    @Autowired
    private com.wd.api.repository.PortalUserRepository portalUserRepository;

    @Autowired
    private com.wd.api.repository.CustomerUserRepository customerUserRepository;

    @Autowired
    private com.wd.api.repository.CustomerProjectRepository customerProjectRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

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
            com.wd.api.model.PortalUser user = portalUserRepository.findById(lead.getAssignedToId()).orElse(null);
            if (user != null) {
                lead.setAssignedTo(user);
                lead.setAssignedTeam(user.getFirstName() + " " + user.getLastName());
            }
        }

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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return savedLead;
    }

    @Transactional
    public Leads updateLead(Long id, Leads leadDetails) {
        return leadsRepository.findById(id).map(lead -> {
            String oldStatus = lead.getLeadStatus();
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
            lead.setUpdatedAt(java.time.LocalDateTime.now());

            // Handle Assignment Update
            if (leadDetails.getAssignedToId() != null) {
                if (!leadDetails.getAssignedToId().equals(oldAssignedId)) {
                    com.wd.api.model.PortalUser user = portalUserRepository.findById(leadDetails.getAssignedToId())
                            .orElse(null);
                    if (user != null) {
                        lead.setAssignedTo(user);
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
        if (leadsRepository.existsById(id)) {
            leadsRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<com.wd.api.model.ActivityFeed> getLeadActivities(Long leadId) {
        return activityFeedService.getActivitiesForLead(leadId);
    }

    public Leads getLeadById(Long id) {
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

    // Analytics (Migrated from DAO)
    public Map<String, Object> getLeadAnalytics() {
        Map<String, Object> analytics = new java.util.HashMap<>();

        String statusSql = "SELECT lead_status, COUNT(*) as count FROM leads GROUP BY lead_status";
        analytics.put("statusDistribution", jdbcTemplate.queryForList(statusSql));

        String sourceSql = "SELECT lead_source, COUNT(*) as count FROM leads GROUP BY lead_source";
        analytics.put("sourceDistribution", jdbcTemplate.queryForList(sourceSql));

        String prioritySql = "SELECT priority, COUNT(*) as count FROM leads GROUP BY priority";
        analytics.put("priorityDistribution", jdbcTemplate.queryForList(prioritySql));

        String trendSql = "SELECT DATE_TRUNC('month', created_at) as month, COUNT(*) as count FROM leads GROUP BY DATE_TRUNC('month', created_at) ORDER BY month DESC LIMIT 12";
        analytics.put("monthlyTrends", jdbcTemplate.queryForList(trendSql));

        return analytics;
    }

    public Map<String, Object> getLeadConversionMetrics() {
        Map<String, Object> metrics = new java.util.HashMap<>();

        String totalLeadsSql = "SELECT COUNT(*) FROM leads";
        Integer totalLeads = jdbcTemplate.queryForObject(totalLeadsSql, Integer.class);
        metrics.put("totalLeads", totalLeads != null ? totalLeads : 0);

        String convertedSql = "SELECT COUNT(*) FROM leads WHERE lead_status = 'converted'";
        Integer convertedLeads = jdbcTemplate.queryForObject(convertedSql, Integer.class);
        metrics.put("convertedLeads", convertedLeads != null ? convertedLeads : 0);

        if (totalLeads != null && totalLeads > 0) {
            double conversionRate = (convertedLeads != null ? convertedLeads : 0) * 100.0 / totalLeads;
            metrics.put("conversionRate", Math.round(conversionRate * 100.0) / 100.0);
        } else {
            metrics.put("conversionRate", 0.0);
        }

        return metrics;
    }

    public List<Leads> getOverdueFollowUps() {
        String sql = "SELECT * FROM leads WHERE next_follow_up < CURRENT_TIMESTAMP AND lead_status NOT IN ('converted', 'lost') ORDER BY next_follow_up ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Leads lead = new Leads();
            lead.setId(rs.getLong("lead_id"));
            lead.setName(rs.getString("name"));
            lead.setEmail(rs.getString("email"));
            // ... minimal mapping or full mapping.
            return lead;
        });
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
            com.wd.api.model.User convertedBy) {
        Leads lead = leadsRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        if ("WON".equalsIgnoreCase(lead.getLeadStatus())) {
            throw new RuntimeException("Lead is already converted");
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
        project.setProjectPhase("planning"); // Default phase
        project.setState("Active");
        project.setLocation(request.getLocation() != null ? request.getLocation() : lead.getLocation());
        project.setSqFeet(lead.getProjectSqftArea() != null ? lead.getProjectSqftArea().doubleValue() : 0.0);
        project.setProjectType(request.getProjectType());

        // Assign Project Manager if selected
        if (request.getProjectManagerId() != null) {
            project.setProjectManagerId(request.getProjectManagerId());
        }
        project.setCreatedBy(convertedBy.getUsername());

        // Set conversion tracking metadata
        project.setConvertedById(convertedBy.getId());
        // convertedAt is automatically set by database trigger, but we can set it
        // explicitly too
        project.setConvertedAt(java.time.LocalDateTime.now());

        project = customerProjectRepository.save(project);

        // 3. Update Lead
        lead.setLeadStatus("WON");
        leadsRepository.save(lead);

        // 4. Log Activity
        try {
            activityFeedService.logSystemActivity(
                    "LEAD_CONVERTED",
                    "Lead Converted",
                    "Lead converted to Project: " + project.getName(),
                    lead.getId(),
                    "LEAD");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return project;
    }

    private com.wd.api.model.CustomerUser createCustomerFromLead(Leads lead) {
        com.wd.api.model.CustomerUser customer = new com.wd.api.model.CustomerUser();
        customer.setEmail(lead.getEmail());
        customer.setFirstName(lead.getName()); // Basic mapping
        customer.setEnabled(true);
        // Set temp password or handle via invitation flow
        customer.setPassword(passwordEncoder.encode("Welcome@123"));
        return customerUserRepository.save(customer);
    }
}
