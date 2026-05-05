package com.wd.api.controller.scheduling;

import com.wd.api.service.scheduling.WbsTemplateService;
import com.wd.api.service.scheduling.dto.WbsTemplateDto;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin endpoints for authoring versioned WBS templates. All endpoints
 * require an authenticated user; per-route authority gates use the
 * permissions seeded by V117 (WBS_TEMPLATE_VIEW / WBS_TEMPLATE_MANAGE).
 */
@RestController
@RequestMapping("/api/wbs/templates")
@PreAuthorize("isAuthenticated()")
public class WbsTemplateAdminController {

    private final WbsTemplateService service;

    public WbsTemplateAdminController(WbsTemplateService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WBS_TEMPLATE_VIEW')")
    public List<WbsTemplateDto> list(
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
        return service.list(includeInactive);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WBS_TEMPLATE_VIEW')")
    public WbsTemplateDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/by-code/{code}")
    @PreAuthorize("hasAuthority('WBS_TEMPLATE_VIEW')")
    public WbsTemplateDto getActive(@PathVariable String code) {
        return service.getActiveByCode(code);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WBS_TEMPLATE_MANAGE')")
    public ResponseEntity<WbsTemplateDto> create(@Valid @RequestBody WbsTemplateDto req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WBS_TEMPLATE_MANAGE')")
    public WbsTemplateDto update(@PathVariable Long id, @Valid @RequestBody WbsTemplateDto req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WBS_TEMPLATE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}
