package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.BoqSearchFilter;
import com.wd.api.model.BoqItem;
import com.wd.api.model.BoqWorkType;
import com.wd.api.service.BoqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boq")
public class BoqController {

    @Autowired
    private BoqService boqService;

    @GetMapping("/search")
    public ResponseEntity<Page<BoqItem>> searchBoqItems(@ModelAttribute BoqSearchFilter filter) {
        try {
            Page<BoqItem> items = boqService.searchBoqItems(filter);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<BoqItem>>> getProjectBoq(@PathVariable Long projectId) {
        try {
            List<BoqItem> items = boqService.getProjectBoq(projectId);
            return ResponseEntity.ok(ApiResponse.success("BoQ items retrieved successfully", items));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch BoQ items: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BoqItem>> createBoqItem(@RequestBody Map<String, Object> request) {
        try {
            BoqItem created = boqService.createBoqItem(request);
            return ResponseEntity.status(201)
                    .body(ApiResponse.success("BoQ item created successfully", created));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Failed to create BoQ item: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BoqItem>> updateBoqItem(@PathVariable Long id,
            @RequestBody Map<String, Object> details) {
        try {
            BoqItem updated = boqService.updateBoqItem(id, details);
            return ResponseEntity.ok(ApiResponse.success("BoQ item updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to update BoQ item: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBoqItem(@PathVariable Long id) {
        try {
            boqService.deleteBoqItem(id);
            return ResponseEntity.ok(ApiResponse.success("BoQ item deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to delete BoQ item: " + e.getMessage()));
        }
    }

    @GetMapping("/work-types")
    public ResponseEntity<ApiResponse<List<BoqWorkType>>> getWorkTypes() {
        try {
            List<BoqWorkType> workTypes = boqService.getAllWorkTypes();
            return ResponseEntity.ok(ApiResponse.success("Work types retrieved successfully", workTypes));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch work types: " + e.getMessage()));
        }
    }

    @GetMapping("/project/{projectId}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProjectSummary(@PathVariable Long projectId) {
        try {
            Map<String, Object> summary = boqService.getProjectSummary(projectId);
            return ResponseEntity.ok(ApiResponse.success("BoQ summary retrieved successfully", summary));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch BoQ summary: " + e.getMessage()));
        }
    }
}
