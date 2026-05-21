package com.wd.api.controller;

import com.wd.api.dto.SiteVisitViolationDTO;
import com.wd.api.repository.SiteVisitViolationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Portal-only endpoint for the geofence-violation audit log.
 *
 * IMPORTANT: this resource is intentionally NOT mirrored in customer-api.
 * Employee compliance data must not be visible to customers.
 */
@RestController
@RequestMapping("/api/site-visit-violations")
public class SiteVisitViolationController {

    private final SiteVisitViolationRepository repository;

    public SiteVisitViolationController(SiteVisitViolationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SITE_VISIT_VIOLATION_VIEW')")
    public ResponseEntity<Page<SiteVisitViolationDTO>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<SiteVisitViolationDTO> result = repository
                .search(projectId, userId, from, to, PageRequest.of(page, safeSize))
                .map(SiteVisitViolationDTO::from);
        return ResponseEntity.ok(result);
    }
}
