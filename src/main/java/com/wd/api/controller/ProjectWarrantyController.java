package com.wd.api.controller;

import com.wd.api.dto.ProjectWarrantyRequest;
import com.wd.api.dto.ProjectWarrantySearchFilter;
import com.wd.api.model.ProjectWarranty;
import com.wd.api.service.ProjectWarrantyService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/warranties")
public class ProjectWarrantyController {

    private final ProjectWarrantyService warrantyService;

    public ProjectWarrantyController(ProjectWarrantyService warrantyService) {
        this.warrantyService = warrantyService;
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProjectWarranty>> searchProjectWarranties(@ModelAttribute ProjectWarrantySearchFilter filter) {
        return ResponseEntity.ok(warrantyService.searchProjectWarranties(filter));
    }

    /**
     * Returns all warranties for a project as a flat list.
     *
     * @deprecated Use {@link #searchProjectWarranties(ProjectWarrantySearchFilter)}
     *             (GET /search) instead, which supports pagination and filtering.
     */
    @GetMapping
    @Deprecated(since = "2026-06")
    public ResponseEntity<List<ProjectWarranty>> getWarranties(@PathVariable Long projectId) {
        return ResponseEntity.ok(warrantyService.getWarrantiesByProject(projectId));
    }

    @PostMapping
    public ResponseEntity<ProjectWarranty> createWarranty(
            @PathVariable Long projectId,
            @RequestBody ProjectWarrantyRequest warranty) {
        return ResponseEntity.ok(warrantyService.createWarranty(warranty.toEntity(), projectId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectWarranty> updateWarranty(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestBody ProjectWarrantyRequest warranty) {
        return ResponseEntity.ok(warrantyService.updateWarranty(id, warranty.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarranty(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        warrantyService.deleteWarranty(id);
        return ResponseEntity.noContent().build();
    }
}
