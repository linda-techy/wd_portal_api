package com.wd.api.controller;

import com.wd.api.dto.FeedbackAnalyticsDto;
import com.wd.api.dto.FeedbackFormDto;
import com.wd.api.dto.FeedbackResponseDto;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.service.FeedbackAnalyticsService;
import com.wd.api.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private static final String KEY_FORM_SCHEMA = "formSchema";

    private final FeedbackService feedbackService;
    private final FeedbackAnalyticsService feedbackAnalyticsService;
    private final PortalUserRepository portalUserRepository;

    // ==================== FORM ENDPOINTS ====================

    /**
     * Create a new feedback form for a project.
     */
    @PostMapping("/forms")
    public ResponseEntity<FeedbackFormDto> createForm(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Long projectId = Long.valueOf(request.get("projectId").toString());
        String title = (String) request.get("title");
        String description = (String) request.get("description");
        String formSchema = request.get(KEY_FORM_SCHEMA) != null 
                ? request.get(KEY_FORM_SCHEMA).toString() 
                : null;
        
        String email = authentication.getName();
        
        FeedbackFormDto created = feedbackService.createForm(
                projectId, title, description, formSchema, email);
        
        return ResponseEntity.ok(created);
    }

    /**
     * Get all feedback forms for a project.
     */
    @GetMapping("/forms/project/{projectId}")
    public ResponseEntity<List<FeedbackFormDto>> getProjectForms(
            @PathVariable Long projectId,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        
        List<FeedbackFormDto> forms = feedbackService.getProjectForms(projectId, activeOnly);
        return ResponseEntity.ok(forms);
    }

    /**
     * Get a specific feedback form by ID.
     */
    @GetMapping("/forms/{formId}")
    public ResponseEntity<FeedbackFormDto> getForm(@PathVariable Long formId) {
        FeedbackFormDto form = feedbackService.getFormById(formId);
        return ResponseEntity.ok(form);
    }

    /**
     * Update a feedback form.
     */
    @PutMapping("/forms/{formId}")
    public ResponseEntity<FeedbackFormDto> updateForm(
            @PathVariable Long formId,
            @RequestBody Map<String, Object> request) {
        
        String title = (String) request.get("title");
        String description = (String) request.get("description");
        String formSchema = request.get(KEY_FORM_SCHEMA) != null 
                ? request.get(KEY_FORM_SCHEMA).toString() 
                : null;
        Boolean isActive = request.get("isActive") != null 
                ? Boolean.valueOf(request.get("isActive").toString()) 
                : null;
        
        FeedbackFormDto updated = feedbackService.updateForm(
                formId, title, description, formSchema, isActive);
        
        return ResponseEntity.ok(updated);
    }

    /**
     * Deactivate a feedback form (soft delete).
     */
    @DeleteMapping("/forms/{formId}")
    public ResponseEntity<Map<String, Object>> deleteForm(@PathVariable Long formId) {
        feedbackService.deleteForm(formId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Feedback form deleted/deactivated successfully"
        ));
    }

    // ==================== RESPONSE ENDPOINTS ====================

    /**
     * Get all responses for a specific form.
     */
    @GetMapping("/forms/{formId}/responses")
    public ResponseEntity<List<FeedbackResponseDto>> getFormResponses(@PathVariable Long formId) {
        List<FeedbackResponseDto> responses = feedbackService.getFormResponses(formId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get all feedback responses for a project.
     */
    @GetMapping("/responses/project/{projectId}")
    public ResponseEntity<List<FeedbackResponseDto>> getProjectResponses(@PathVariable Long projectId) {
        List<FeedbackResponseDto> responses = feedbackService.getProjectResponses(projectId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get a specific response by ID.
     */
    @GetMapping("/responses/{responseId}")
    public ResponseEntity<FeedbackResponseDto> getResponse(@PathVariable Long responseId) {
        FeedbackResponseDto response = feedbackService.getResponseById(responseId);
        return ResponseEntity.ok(response);
    }

    // ==================== ADMIN REPLY ENDPOINT ====================

    /**
     * Admin replies to a customer's feedback response.
     *
     * <p>POST /api/feedback/responses/{responseId}/reply
     * Body: { "message": "..." }
     */
    @PostMapping("/responses/{responseId}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeedbackResponseDto> replyToResponse(
            @PathVariable Long responseId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String message = (String) request.get("message");
        Long adminUserId = portalUserRepository.findByEmail(authentication.getName())
                .map(PortalUser::getId)
                .orElse(null);

        FeedbackResponseDto updated = feedbackService.replyToResponse(responseId, message, adminUserId);
        return ResponseEntity.ok(updated);
    }

    // ==================== STATISTICS ENDPOINTS ====================

    /**
     * Get response count for a form.
     */
    @GetMapping("/forms/{formId}/count")
    public ResponseEntity<Map<String, Object>> getFormResponseCount(@PathVariable Long formId) {
        Long count = feedbackService.getFormResponseCount(formId);
        return ResponseEntity.ok(Map.of("formId", formId, "responseCount", count));
    }

    /**
     * Get active form count for a project.
     */
    @GetMapping("/forms/project/{projectId}/count")
    public ResponseEntity<Map<String, Object>> getActiveFormCount(@PathVariable Long projectId) {
        Long count = feedbackService.getActiveFormCount(projectId);
        return ResponseEntity.ok(Map.of("projectId", projectId, "activeFormCount", count));
    }

    // ==================== ANALYTICS ENDPOINTS ====================

    /**
     * Schema-aware analytics for a single feedback form.
     *
     * <p>Returns per-question averages, distributions, and NPS (when applicable).
     * Uses the same authentication as other feedback read endpoints.
     *
     * <p>GET /api/feedback/forms/{formId}/analytics
     */
    @GetMapping("/forms/{formId}/analytics")
    public ResponseEntity<FeedbackAnalyticsDto> getFormAnalytics(@PathVariable Long formId) {
        FeedbackAnalyticsDto analytics = feedbackAnalyticsService.analyseForm(formId);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Project-level analytics rollup across all feedback forms for a project.
     *
     * <p>GET /api/feedback/responses/project/{projectId}/analytics
     */
    @GetMapping("/responses/project/{projectId}/analytics")
    public ResponseEntity<FeedbackAnalyticsDto> getProjectAnalytics(@PathVariable Long projectId) {
        FeedbackAnalyticsDto analytics = feedbackAnalyticsService.analyseProject(projectId);
        return ResponseEntity.ok(analytics);
    }
}
