package com.wd.api.dto;

import com.wd.api.model.LeadScoreHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for LeadScoreHistory API responses
 * Prevents lazy-loading serialization issues
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadScoreHistoryDTO {
    private Long id;
    private Long leadId;
    private Integer previousScore;
    private Integer newScore;
    private String previousCategory;
    private String newCategory;
    private String scoreFactors;
    private String reason;
    private LocalDateTime scoredAt;
    private Long scoredById;
    private String scoredByName;

    public LeadScoreHistoryDTO(LeadScoreHistory history) {
        this.id = history.getId();
        this.leadId = history.getLeadId();
        this.previousScore = history.getPreviousScore();
        this.newScore = history.getNewScore();
        this.previousCategory = history.getPreviousCategory();
        this.newCategory = history.getNewCategory();
        this.scoreFactors = history.getScoreFactors();
        this.reason = history.getReason();
        this.scoredAt = history.getScoredAt();
        this.scoredById = history.getScoredById();
        
        // Safely extract scored by name (avoid lazy-loading issues)
        if (history.getScoredBy() != null) {
            this.scoredByName = history.getScoredBy().getEmail();
        }
    }
}
