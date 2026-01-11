package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.model.QualityCheck;
import com.wd.api.model.PortalUser;
import com.wd.api.service.QualityCheckService;
import com.wd.api.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quality-checks")
public class QualityCheckController {

    @Autowired
    private QualityCheckService qualityCheckService;

    @Autowired
    private AuthService authService;

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<QualityCheck>>> getProjectChecks(@PathVariable Long projectId) {
        try {
            List<QualityCheck> checks = qualityCheckService.getProjectChecks(projectId);
            return ResponseEntity.ok(ApiResponse.success("Quality checks retrieved successfully", checks));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch quality checks: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QualityCheck>> createCheck(@RequestBody Map<String, Object> payload) {
        try {
            PortalUser currentUser = authService.getCurrentUser();

            QualityCheck check = new QualityCheck();
            check.setTitle((String) payload.get("title"));
            check.setDescription((String) payload.get("description"));
            check.setStatus((String) payload.get("status"));
            check.setResult((String) payload.get("result"));
            check.setRemarks((String) payload.get("remarks"));

            Long projectId = Long.valueOf(payload.get("projectId").toString());

            QualityCheck created = qualityCheckService.createCheck(check, projectId, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Quality check created successfully", created));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to create quality check: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QualityCheck>> updateCheck(@PathVariable Long id,
            @RequestBody QualityCheck checkDetails) {
        try {
            QualityCheck updated = qualityCheckService.updateCheck(id, checkDetails);
            return ResponseEntity.ok(ApiResponse.success("Quality check updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to update quality check: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCheck(@PathVariable Long id) {
        try {
            qualityCheckService.deleteCheck(id);
            return ResponseEntity.ok(ApiResponse.success("Quality check deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to delete quality check: " + e.getMessage()));
        }
    }
}
