package com.wd.api.controller;

import com.wd.api.model.DesignPackageTemplate;
import com.wd.api.model.PortalUser;
import com.wd.api.service.DesignPackageTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin CRUD for the Design Package Template catalog.
 * Read endpoints additionally accept callers with the PROJECT_VIEW gate so
 * the Create-Design-Payment dialog can fetch the active list for its
 * dropdown without granting full DESIGN_PACKAGE_VIEW to every staff role.
 */
@RestController
@RequestMapping("/api/design-package-templates")
public class DesignPackageTemplateController {

    private static final Logger logger = LoggerFactory.getLogger(DesignPackageTemplateController.class);

    private final DesignPackageTemplateService service;

    public DesignPackageTemplateController(DesignPackageTemplateService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('DESIGN_PACKAGE_VIEW', 'DESIGN_PACKAGE_MANAGE', 'PROJECT_VIEW')")
    public ResponseEntity<?> list(
            @RequestParam(value = "activeOnly", defaultValue = "false") boolean activeOnly) {
        List<DesignPackageTemplate> rows = activeOnly ? service.listActive() : service.listAll();
        return ResponseEntity.ok(Map.of("success", true, "data", rows));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('DESIGN_PACKAGE_VIEW', 'DESIGN_PACKAGE_MANAGE', 'PROJECT_VIEW')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", service.getById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DESIGN_PACKAGE_MANAGE')")
    public ResponseEntity<?> create(@RequestBody DesignPackageTemplate body, Authentication auth) {
        try {
            DesignPackageTemplate saved = service.create(body, userId(auth));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Design package template created",
                    "data", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating design package template", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Failed to create template"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DESIGN_PACKAGE_MANAGE')")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody DesignPackageTemplate body,
                                    Authentication auth) {
        try {
            DesignPackageTemplate saved = service.update(id, body, userId(auth));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Design package template updated",
                    "data", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating design package template {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Failed to update template"));
        }
    }

    /**
     * Soft archive — recommended over DELETE because historical
     * design_package_payments rows still reference packageName.
     */
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAuthority('DESIGN_PACKAGE_MANAGE')")
    public ResponseEntity<?> setActive(@PathVariable Long id,
                                       @RequestBody Map<String, Object> body,
                                       Authentication auth) {
        try {
            boolean active = Boolean.TRUE.equals(body.get("active"));
            DesignPackageTemplate saved = service.setActive(id, active, userId(auth));
            return ResponseEntity.ok(Map.of("success", true, "data", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DESIGN_PACKAGE_MANAGE')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(Map.of(
                    "success", true, "message", "Design package template deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private Long userId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof PortalUser u) {
            return u.getId();
        }
        return null;
    }
}
