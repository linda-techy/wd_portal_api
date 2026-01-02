package com.wd.api.controller;

import com.wd.api.model.DelayLog;
import com.wd.api.service.DelayLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/delays")
public class DelayLogController {

    @Autowired
    private DelayLogService delayLogService;

    @GetMapping
    public ResponseEntity<List<DelayLog>> getDelays(@PathVariable Long projectId) {
        return ResponseEntity.ok(delayLogService.getDelaysByProject(projectId));
    }

    @PostMapping
    public ResponseEntity<DelayLog> logDelay(
            @PathVariable Long projectId,
            @RequestBody DelayLog delay) {
        // TODO: Get real user ID
        Long userId = 1L;
        return ResponseEntity.ok(delayLogService.logDelay(delay, projectId, userId));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<DelayLog> closeDelay(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(delayLogService.closeDelay(id, endDate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDelay(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        delayLogService.deleteDelay(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/impact")
    public ResponseEntity<Map<String, Long>> getImpactAnalysis(@PathVariable Long projectId) {
        return ResponseEntity.ok(delayLogService.getDelayImpactAnalysis(projectId));
    }
}
