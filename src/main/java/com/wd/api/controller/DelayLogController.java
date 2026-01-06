package com.wd.api.controller;

import com.wd.api.model.DelayLog;
import com.wd.api.model.User;
import com.wd.api.repository.UserRepository;
import com.wd.api.service.DelayLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/delays")
public class DelayLogController {

    @Autowired
    private DelayLogService delayLogService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<DelayLog>> getDelays(@PathVariable Long projectId) {
        return ResponseEntity.ok(delayLogService.getDelaysByProject(projectId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DelayLog> logDelay(
            @PathVariable Long projectId,
            @RequestBody DelayLog delay,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ResponseEntity.ok(delayLogService.logDelay(delay, projectId, userId));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DelayLog> closeDelay(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(delayLogService.closeDelay(id, endDate));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> deleteDelay(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        delayLogService.deleteDelay(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/impact")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Long>> getImpactAnalysis(@PathVariable Long projectId) {
        return ResponseEntity.ok(delayLogService.getDelayImpactAnalysis(projectId));
    }

    private Long getCurrentUserId(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}
