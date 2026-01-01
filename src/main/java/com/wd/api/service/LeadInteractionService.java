package com.wd.api.service;

import com.wd.api.model.LeadInteraction;
import com.wd.api.repository.LeadInteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing lead interactions and communications
 */
@Service
public class LeadInteractionService {

    @Autowired
    private LeadInteractionRepository interactionRepository;

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
    public LeadInteraction createInteraction(LeadInteraction interaction, Long createdById) {
        interaction.setCreatedById(createdById);

        // Set interaction date to now if not provided
        if (interaction.getInteractionDate() == null) {
            interaction.setInteractionDate(LocalDateTime.now());
        }

        // Validate interaction type
        validateInteractionType(interaction.getInteractionType());

        return interactionRepository.save(interaction);
    }

    /**
     * Update an existing interaction
     */
    @Transactional
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
        LeadInteraction interaction = new LeadInteraction();
        interaction.setLeadId(leadId);
        interaction.setInteractionType(type);
        interaction.setNotes(notes);
        interaction.setCreatedById(createdById);
        interaction.setInteractionDate(LocalDateTime.now());

        return interactionRepository.save(interaction);
    }

    /**
     * Schedule a follow-up action
     */
    @Transactional
    public LeadInteraction scheduleFollowUp(Long leadId, String nextAction, LocalDateTime nextActionDate,
            Long createdById) {
        LeadInteraction interaction = new LeadInteraction();
        interaction.setLeadId(leadId);
        interaction.setInteractionType("OTHER");
        interaction.setSubject("Follow-up scheduled");
        interaction.setNextAction(nextAction);
        interaction.setNextActionDate(nextActionDate);
        interaction.setCreatedById(createdById);
        interaction.setInteractionDate(LocalDateTime.now());

        return interactionRepository.save(interaction);
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
