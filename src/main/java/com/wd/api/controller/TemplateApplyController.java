package com.wd.api.controller;

import com.wd.api.dto.TemplateApplyRequest;
import com.wd.api.service.TemplateApplyService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/templates")
public class TemplateApplyController {

    private static final Logger logger = LoggerFactory.getLogger(TemplateApplyController.class);

    private final TemplateApplyService service;

    public TemplateApplyController(TemplateApplyService service) {
        this.service = service;
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAuthority('PROJECT_EDIT')")
    public ResponseEntity<?> apply(
            @PathVariable Long projectId,
            @Valid @RequestBody TemplateApplyRequest request) {
        try {
            TemplateApplyService.Result result = service.apply(projectId, request.templateCode());
            return ResponseEntity.ok(Map.of(
                    "created", result.created(),
                    "milestoneCount", result.milestoneCount()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Template apply failed for project {}", projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Apply failed"));
        }
    }
}
