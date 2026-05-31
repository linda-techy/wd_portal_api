package com.wd.api.dto;

import com.wd.api.model.LeadInteraction;
import java.time.LocalDateTime;

public record LeadInteractionRequest(
        Long leadId, String interactionType, LocalDateTime interactionDate, Integer durationMinutes,
        String subject, String notes, String outcome, String nextAction, LocalDateTime nextActionDate,
        String location, String metadata) {
    public LeadInteraction toEntity() {
        LeadInteraction i = new LeadInteraction();
        i.setLeadId(leadId);
        i.setInteractionType(interactionType);
        i.setInteractionDate(interactionDate);
        i.setDurationMinutes(durationMinutes);
        i.setSubject(subject);
        i.setNotes(notes);
        i.setOutcome(outcome);
        i.setNextAction(nextAction);
        i.setNextActionDate(nextActionDate);
        i.setLocation(location);
        i.setMetadata(metadata);
        return i;
    }
}
