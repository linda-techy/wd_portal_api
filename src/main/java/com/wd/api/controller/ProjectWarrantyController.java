package com.wd.api.controller;

import com.wd.api.model.ProjectWarranty;
import com.wd.api.service.ProjectWarrantyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/warranties")
public class ProjectWarrantyController {

    @Autowired
    private ProjectWarrantyService warrantyService;

    @GetMapping
    public ResponseEntity<List<ProjectWarranty>> getWarranties(@PathVariable Long projectId) {
        return ResponseEntity.ok(warrantyService.getWarrantiesByProject(projectId));
    }

    @PostMapping
    public ResponseEntity<ProjectWarranty> createWarranty(
            @PathVariable Long projectId,
            @RequestBody ProjectWarranty warranty) {
        return ResponseEntity.ok(warrantyService.createWarranty(warranty, projectId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectWarranty> updateWarranty(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestBody ProjectWarranty warranty) {
        return ResponseEntity.ok(warrantyService.updateWarranty(id, warranty));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarranty(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        warrantyService.deleteWarranty(id);
        return ResponseEntity.noContent().build();
    }
}
