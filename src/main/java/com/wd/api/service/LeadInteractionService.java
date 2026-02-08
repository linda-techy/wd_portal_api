package com.wd.api.service;

import com.wd.api.dto.LeadInteractionSearchFilter;
import com.wd.api.model.LeadInteraction;
import com.wd.api.repository.LeadInteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing lead interactions and communications
 */
@Service
public class LeadInteractionService {

    @Autowired
    private LeadInteractionRepository interactionRepository;

    @Autowired
    private com.wd.api.repository.LeadRepository leadRepository;

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<LeadInteraction> searchLeadInteractions(LeadInteractionSearchFilter filter) {
        Specification<LeadInteraction> spec = buildSpecification(filter);
        return interactionRepository.findAll(spec, filter.toPageable());
    }

    private Specification<LeadInteraction> buildSpecification(LeadInteractionSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search across notes, subject, interaction type
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("notes")), searchPattern),
                        cb.like(cb.lower(root.get("subject")), searchPattern),
                        cb.like(cb.lower(root.get("interactionType")), searchPattern)));
            }

            // Filter by leadId
            if (filter.getLeadId() != null) {
                predicates.add(cb.equal(root.get("leadId"), filter.getLeadId()));
            }

            // Filter by interactionType
            if (filter.getInteractionType() != null && !filter.getInteractionType().isEmpty()) {
                predicates.add(cb.equal(root.get("interactionType"), filter.getInteractionType()));
            }

            // Filter by userId (createdById)
            if (filter.getUserId() != null) {
                predicates.add(cb.equal(root.get("createdById"), filter.getUserId()));
            }

            // Filter by outcome
            if (filter.getOutcome() != null && !filter.getOutcome().isEmpty()) {
                predicates.add(cb.equal(root.get("outcome"), filter.getOutcome()));
            }

            // Filter by followUpRequired
            if (filter.getFollowUpRequired() != null) {
                if (filter.getFollowUpRequired()) {
                    predicates.add(cb.isNotNull(root.get("nextActionDate")));
                } else {
                    predicates.add(cb.isNull(root.get("nextActionDate")));
                }
            }

            // Date range filter
            if (filter.getStartDate() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("interactionDate"), filter.getStartDate().atStartOfDay()));
            }
            if (filter.getEndDate() != null) {
                predicates
                        .add(cb.lessThanOrEqualTo(root.get("interactionDate"), filter.getEndDate().atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Get all interactions for a specific lead
     */
    public List<LeadInteraction> getInteractionsByLeadId(Long leadId) {
        return interactionRepository.findByLeadIdOrderByInteractionDateDesc(leadId);
    }

    /**
     * Get upcoming actions within a date range
     */
    public List<LeadInteraction> getUpcomingActions(LocalDateTime startDate, LocalDateTime endDate) {
        return interactionRepository.findUpcomingActions(startDate, endDate);
    }

    /**
     * Get overdue actions
     */
    public List<LeadInteraction> getOverdueActions() {
        return interactionRepository.findOverdueActions(LocalDateTime.now());
    }

    /**
     * Get interactions by type
     */
    public List<LeadInteraction> getInteractionsByType(String interactionType) {
        return interactionRepository.findByInteractionType(interactionType);
    }

    /**
     * Create a new interaction
     */
    @Transactional
    @SuppressWarnings("null")
    public LeadInteraction createInteraction(LeadInteraction interaction, Long createdById) {
        interaction.setCreatedById(createdById);

        // Set interaction date to now if not provided
        final LocalDateTime interactionDate;
        if (interaction.getInteractionDate() == null) {
            interactionDate = LocalDateTime.now();
            interaction.setInteractionDate(interactionDate);
        } else {
            interactionDate = interaction.getInteractionDate();
        }

        // Validate interaction type
        validateInteractionType(interaction.getInteractionType());

        LeadInteraction saved = interactionRepository.save(interaction);

        // Update lead's last_contact_date when interaction is created
        if (interaction.getLeadId() != null) {
            leadRepository.findById(interaction.getLeadId()).ifPresent(lead -> {
                lead.setLastContactDate(interactionDate);
                leadRepository.save(lead);
            });
        }

        return saved;
    }

    /**
     * Update an existing interaction
     */
    @Transactional
    @SuppressWarnings("null")
    public LeadInteraction updateInteraction(Long id, LeadInteraction updatedInteraction) {
        LeadInteraction existing = interactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Interaction not found"));

        existing.setInteractionType(updatedInteraction.getInteractionType());
        existing.setInteractionDate(updatedInteraction.getInteractionDate());
        existing.setDurationMinutes(updatedInteraction.getDurationMinutes());
        existing.setSubject(updatedInteraction.getSubject());
        existing.setNotes(updatedInteraction.getNotes());
        existing.setOutcome(updatedInteraction.getOutcome());
        existing.setNextAction(updatedInteraction.getNextAction());
        existing.setNextActionDate(updatedInteraction.getNextActionDate());

        return interactionRepository.save(existing);
    }

    /**
     * Delete an interaction
     */
    @Transactional
    @SuppressWarnings("null")
    public void deleteInteraction(Long id) {
        if (!interactionRepository.existsById(id)) {
            throw new RuntimeException("Interaction not found");
        }
        interactionRepository.deleteById(id);
    }

    /**
     * Log a quick interaction (simplified method)
     */
    @Transactional
    public LeadInteraction logQuickInteraction(Long leadId, String type, String notes, Long createdById) {
        LocalDateTime now = LocalDateTime.now();
        LeadInteraction interaction = new LeadInteraction();
        interaction.setLeadId(leadId);
        interaction.setInteractionType(type);
        interaction.setNotes(notes);
        interaction.setCreatedById(createdById);
        interaction.setInteractionDate(now);

        LeadInteraction saved = interactionRepository.save(interaction);

        // Update lead's last_contact_date
        if (leadId != null) {
            leadRepository.findById(leadId).ifPresent(lead -> {
                lead.setLastContactDate(now);
                leadRepository.save(lead);
            });
        }

        return saved;
    }

    /**
     * Schedule a follow-up action
     */
    @Transactional
    public LeadInteraction scheduleFollowUp(Long leadId, String nextAction, LocalDateTime nextActionDate,
            Long createdById) {
        LocalDateTime now = LocalDateTime.now();
        LeadInteraction interaction = new LeadInteraction();
        interaction.setLeadId(leadId);
        interaction.setInteractionType("OTHER");
        interaction.setSubject("Follow-up scheduled");
        interaction.setNextAction(nextAction);
        interaction.setNextActionDate(nextActionDate);
        interaction.setCreatedById(createdById);
        interaction.setInteractionDate(now);

        LeadInteraction saved = interactionRepository.save(interaction);

        // Update lead's last_contact_date and next_follow_up
        if (leadId != null) {
            leadRepository.findById(leadId).ifPresent(lead -> {
                lead.setLastContactDate(now);
                lead.setNextFollowUp(nextActionDate);
                leadRepository.save(lead);
            });
        }

        return saved;
    }

    /**
     * Validate interaction type
     */
    private void validateInteractionType(String type) {
        List<String> validTypes = List.of("CALL", "EMAIL", "MEETING", "SITE_VISIT", "WHATSAPP", "SMS", "OTHER");
        if (!validTypes.contains(type)) {
            throw new IllegalArgumentException("Invalid interaction type: " + type);
        }
    }

    /**
     * Get interaction statistics for a lead
     */
    public java.util.Map<String, Object> getInteractionStats(Long leadId) {
        List<LeadInteraction> interactions = getInteractionsByLeadId(leadId);

        long totalInteractions = interactions.size();
        long callsCount = interactions.stream().filter(i -> "CALL".equals(i.getInteractionType())).count();
        long meetingsCount = interactions.stream().filter(i -> "MEETING".equals(i.getInteractionType())).count();
        long emailsCount = interactions.stream().filter(i -> "EMAIL".equals(i.getInteractionType())).count();

        LocalDateTime lastInteraction = interactions.isEmpty() ? null : interactions.get(0).getInteractionDate();

        return java.util.Map.of(
                "totalInteractions", totalInteractions,
                "callsCount", callsCount,
                "meetingsCount", meetingsCount,
                "emailsCount", emailsCount,
                "lastInteractionDate", lastInteraction != null ? lastInteraction.toString() : "Never");
    }
}
