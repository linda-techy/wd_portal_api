package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.LeadScoreHistoryDTO;
import com.wd.api.service.LeadScoreHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing lead score history
 * Provides audit trail for lead scoring changes
 */
@RestController
@RequestMapping("/leads/{leadId}/score-history")
@CrossOrigin(origins = "*")
public class LeadScoreHistoryController {

    private static final Logger logger = LoggerFactory.getLogger(LeadScoreHistoryController.class);

    @Autowired
    private LeadScoreHistoryService scoreHistoryService;

    /**
     * Get all score history for a lead
     * Returns chronological list of all score changes
     * 
     * @param leadId The lead ID
     * @return List of score history entries, most recent first
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<LeadScoreHistoryDTO>>> getScoreHistory(
            @PathVariable String leadId) {
        try {
            Long id = Long.parseLong(leadId);
            List<LeadScoreHistoryDTO> history = scoreHistoryService.getScoreHistory(id);
            return ResponseEntity.ok(ApiResponse.success("Score history retrieved successfully", history));
        } catch (NumberFormatException e) {
            logger.error("Invalid lead ID format: {}", leadId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid lead ID format"));
        } catch (Exception e) {
            logger.error("Error fetching score history for lead {}", leadId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get score history for a lead with pagination
     * 
     * @param leadId The lead ID
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20)
     * @return Page of score history entries
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<Page<LeadScoreHistoryDTO>>> getScoreHistoryPaginated(
            @PathVariable String leadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long id = Long.parseLong(leadId);
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scoredAt"));
            Page<LeadScoreHistoryDTO> historyPage = scoreHistoryService.getScoreHistoryPaginated(id, pageable);
            return ResponseEntity.ok(ApiResponse.success("Score history retrieved successfully", historyPage));
        } catch (NumberFormatException e) {
            logger.error("Invalid lead ID format: {}", leadId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid lead ID format"));
        } catch (Exception e) {
            logger.error("Error fetching paginated score history for lead {}", leadId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get the latest score history entry for a lead
     * 
     * @param leadId The lead ID
     * @return Latest score history entry or null if none exists
     */
    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<LeadScoreHistoryDTO>> getLatestScoreHistory(
            @PathVariable String leadId) {
        try {
            Long id = Long.parseLong(leadId);
            LeadScoreHistoryDTO latest = scoreHistoryService.getLatestScoreHistory(id);
            if (latest != null) {
                return ResponseEntity.ok(ApiResponse.success("Latest score history retrieved successfully", latest));
            } else {
                return ResponseEntity.ok(ApiResponse.success("No score history found", null));
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid lead ID format: {}", leadId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid lead ID format"));
        } catch (Exception e) {
            logger.error("Error fetching latest score history for lead {}", leadId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get count of score changes for a lead
     * 
     * @param leadId The lead ID
     * @return Total count of score changes
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<Long>> getScoreHistoryCount(
            @PathVariable String leadId) {
        try {
            Long id = Long.parseLong(leadId);
            long count = scoreHistoryService.countScoreChanges(id);
            return ResponseEntity.ok(ApiResponse.success("Score history count retrieved successfully", count));
        } catch (NumberFormatException e) {
            logger.error("Invalid lead ID format: {}", leadId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid lead ID format"));
        } catch (Exception e) {
            logger.error("Error fetching score history count for lead {}", leadId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error"));
        }
    }
}
