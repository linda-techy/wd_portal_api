package com.wd.api.service;

import com.wd.api.dto.LeadScoreHistoryDTO;
import com.wd.api.model.Lead;
import com.wd.api.model.LeadScoreHistory;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.LeadScoreHistoryRepository;
import com.wd.api.repository.PortalUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing lead score history
 * Provides audit trail for lead scoring changes
 */
@Service
public class LeadScoreHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(LeadScoreHistoryService.class);

    @Autowired
    private LeadScoreHistoryRepository scoreHistoryRepository;

    @Autowired
    private PortalUserRepository portalUserRepository;

    /**
     * Log a score change for a lead
     * Enterprise-grade: Tracks all score changes for audit trail and analysis
     * 
     * @param lead             The lead with updated score
     * @param previousScore    The score before the change
     * @param previousCategory The category before the change
     * @param scoredById       The ID of the user who scored (null for system
     *                         calculations)
     * @param reason           Optional reason for the score change
     * @param scoreFactors     JSON string of scoring factors
     */
    @Transactional
    public void logScoreChange(Lead lead, Integer previousScore, String previousCategory,
            Long scoredById, String reason, String scoreFactors) {
        try {
            LeadScoreHistory history = new LeadScoreHistory();
            history.setLeadId(lead.getId());
            history.setPreviousScore(previousScore);
            history.setNewScore(lead.getScore() != null ? lead.getScore() : 0);
            history.setPreviousCategory(previousCategory != null ? previousCategory : "COLD");
            history.setNewCategory(lead.getScoreCategory() != null ? lead.getScoreCategory() : "COLD");
            history.setScoreFactors(scoreFactors);
            history.setReason(reason);
            history.setScoredById(scoredById);
            history.setScoredAt(java.time.LocalDateTime.now());

            scoreHistoryRepository.save(history);

            logger.debug("Logged score change for lead {}: {} -> {} ({})",
                    lead.getId(), previousScore, history.getNewScore(), history.getNewCategory());
        } catch (Exception e) {
            logger.error("Error logging score change for lead {}: {}", lead.getId(), e.getMessage(), e);
            // Don't throw - score logging should not break lead updates
        }
    }

    /**
     * Get all score history for a lead
     * Returns DTOs to avoid lazy-loading serialization issues
     * 
     * @param leadId The lead ID
     * @return List of score history entries, most recent first
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public List<LeadScoreHistoryDTO> getScoreHistory(Long leadId) {
        List<LeadScoreHistory> historyList = scoreHistoryRepository.findByLeadIdOrderByScoredAtDesc(leadId);

        return historyList.stream()
                .map(history -> {
                    LeadScoreHistoryDTO dto = new LeadScoreHistoryDTO(history);

                    // Safely extract scored by name
                    if (history.getScoredById() != null) {
                        Optional<PortalUser> userOpt = portalUserRepository.findById(history.getScoredById());
                        if (userOpt.isPresent()) {
                            dto.setScoredByName(userOpt.get().getEmail());
                        }
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get score history for a lead with pagination
     * 
     * @param leadId   The lead ID
     * @param pageable Pagination parameters
     * @return Page of score history entries
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<LeadScoreHistoryDTO> getScoreHistoryPaginated(Long leadId, Pageable pageable) {
        Page<LeadScoreHistory> historyPage = scoreHistoryRepository.findByLeadIdOrderByScoredAtDesc(leadId, pageable);

        return historyPage.map(history -> {
            LeadScoreHistoryDTO dto = new LeadScoreHistoryDTO(history);

            // Safely extract scored by name
            if (history.getScoredById() != null) {
                Optional<PortalUser> userOpt = portalUserRepository.findById(history.getScoredById());
                if (userOpt.isPresent()) {
                    dto.setScoredByName(userOpt.get().getEmail());
                }
            }

            return dto;
        });
    }

    /**
     * Get the latest score history entry for a lead
     * 
     * @param leadId The lead ID
     * @return Latest score history entry or null if none exists
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public LeadScoreHistoryDTO getLatestScoreHistory(Long leadId) {
        List<LeadScoreHistory> latest = scoreHistoryRepository.findLatestByLeadId(
                leadId, org.springframework.data.domain.PageRequest.of(0, 1));

        if (latest.isEmpty()) {
            return null;
        }

        LeadScoreHistory history = latest.get(0);
        LeadScoreHistoryDTO dto = new LeadScoreHistoryDTO(history);

        // Safely extract scored by name
        if (history.getScoredById() != null) {
            Optional<PortalUser> userOpt = portalUserRepository.findById(history.getScoredById());
            if (userOpt.isPresent()) {
                dto.setScoredByName(userOpt.get().getEmail());
            }
        }

        return dto;
    }

    /**
     * Count total score changes for a lead
     * 
     * @param leadId The lead ID
     * @return Total number of score changes
     */
    @Transactional(readOnly = true)
    public long countScoreChanges(Long leadId) {
        return scoreHistoryRepository.countByLeadId(leadId);
    }
}
