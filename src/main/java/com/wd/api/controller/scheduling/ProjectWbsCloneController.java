package com.wd.api.controller.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.service.scheduling.WbsTemplateClonerService;
import com.wd.api.service.scheduling.dto.WbsCloneRequest;
import com.wd.api.service.scheduling.dto.WbsCloneResult;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dedicated endpoint for cloning a WBS template into an existing project.
 *
 * <p>Migration path: future versions of CustomerProjectController#create
 * may accept an optional templateId in the create request and call this
 * service inline. For S1 we keep the cloner as a discrete step the
 * scheduler invokes post-create, to limit blast radius on the heavily
 * tested project-creation flow.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/wbs")
@PreAuthorize("isAuthenticated()")
public class ProjectWbsCloneController {

    private final WbsTemplateClonerService cloner;
    private final CustomerProjectRepository projects;

    public ProjectWbsCloneController(WbsTemplateClonerService cloner,
                                     CustomerProjectRepository projects) {
        this.cloner = cloner;
        this.projects = projects;
    }

    @PostMapping("/clone-from-template")
    @PreAuthorize("hasAuthority('PROJECT_WBS_CLONE')")
    public WbsCloneResult clone(
            @PathVariable Long projectId,
            @Valid @RequestBody WbsCloneRequest req) {
        CustomerProject project = projects.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project " + projectId));
        return cloner.cloneInto(project, req.getTemplateId(), req.getFloorCount());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadInput(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
