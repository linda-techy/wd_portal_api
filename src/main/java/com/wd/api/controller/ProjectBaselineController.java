package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.service.scheduling.BaselineAlreadyExistsException;
import com.wd.api.service.scheduling.NoBaselineException;
import com.wd.api.service.scheduling.ProjectBaselineService;
import com.wd.api.service.scheduling.dto.ApproveBaselineResponse;
import com.wd.api.service.scheduling.dto.ProjectBaselineDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/baseline")
public class ProjectBaselineController {

    private final ProjectBaselineService service;
    private final PortalUserRepository portalUserRepository;

    public ProjectBaselineController(ProjectBaselineService service,
                                     PortalUserRepository portalUserRepository) {
        this.service = service;
        this.portalUserRepository = portalUserRepository;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROJECT_BASELINE_APPROVE')")
    public ResponseEntity<ApiResponse<ApproveBaselineResponse>> approve(
            @PathVariable Long projectId,
            Authentication auth) {
        Long approvedBy = resolveCurrentUserId(auth);
        ApproveBaselineResponse resp = service.approve(projectId, approvedBy);
        return ResponseEntity.ok(ApiResponse.success("Baseline approved", resp));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    public ResponseEntity<ApiResponse<ProjectBaselineDto>> get(@PathVariable Long projectId) {
        ProjectBaselineDto dto = service.getBaseline(projectId);
        return ResponseEntity.ok(ApiResponse.success("Baseline retrieved", dto));
    }

    @ExceptionHandler(BaselineAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> onAlreadyExists(BaselineAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(NoBaselineException.class)
    public ResponseEntity<ApiResponse<Void>> onNoBaseline(NoBaselineException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
    }

    /**
     * Resolves the current authenticated user id by looking up the PortalUser
     * by email (the authentication principal name). Returns null only when
     * running with a mock principal that has no matching user row (e.g. in
     * @WithMockUser slice tests where the service call is mocked anyway).
     */
    private Long resolveCurrentUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return portalUserRepository.findByEmail(auth.getName())
                .map(PortalUser::getId)
                .orElse(null);
    }
}
