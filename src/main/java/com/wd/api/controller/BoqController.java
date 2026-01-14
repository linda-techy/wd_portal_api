package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.BoqSearchFilter;
import com.wd.api.model.BoqItem;
import com.wd.api.service.BoqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    @Deprecated
    public ResponseEntity<ApiResponse<List<BoqItem>>> getProjectBoq(@PathVariable Long projectId) {
        try {
            List<BoqItem> items = boqService.getProjectBoq(projectId);
            return ResponseEntity.ok(ApiResponse.success("BoQ items retrieved successfully", items));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch BoQ items: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BoqItem>> updateBoqItem(@PathVariable Long id, @RequestBody BoqItem details) {
        try {
            BoqItem updated = boqService.updateBoqItem(id, details);
            return ResponseEntity.ok(ApiResponse.success("BoQ item updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to update BoQ item: " + e.getMessage()));
        }
    }
}
