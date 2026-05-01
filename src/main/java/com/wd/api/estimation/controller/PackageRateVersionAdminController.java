package com.wd.api.estimation.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.dto.admin.RateVersionCreateRequest;
import com.wd.api.estimation.dto.admin.RateVersionResponse;
import com.wd.api.estimation.service.admin.PackageRateVersionAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/estimation/rate-versions")
@PreAuthorize("isAuthenticated()")
public class PackageRateVersionAdminController {

    private final PackageRateVersionAdminService service;

    public PackageRateVersionAdminController(PackageRateVersionAdminService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ESTIMATION_SETTINGS_VIEW')")
    public ResponseEntity<ApiResponse<List<RateVersionResponse>>> list(
            @RequestParam UUID packageId, @RequestParam ProjectType projectType) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.list(packageId, projectType)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ESTIMATION_SETTINGS_VIEW')")
    public ResponseEntity<ApiResponse<RateVersionResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.get(id)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('ESTIMATION_SETTINGS_VIEW')")
    public ResponseEntity<ApiResponse<RateVersionResponse>> getActive(
            @RequestParam UUID packageId, @RequestParam ProjectType projectType) {
        Optional<RateVersionResponse> active = service.getActive(packageId, projectType);
        return active
                .map(rv -> ResponseEntity.ok(ApiResponse.success("OK", rv)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<RateVersionResponse>error("No active rate version for given package + project type")));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ESTIMATION_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<RateVersionResponse>> create(
            @Valid @RequestBody RateVersionCreateRequest req) {
        RateVersionResponse created = service.createNewVersion(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rate version created (previous active version closed atomically)", created));
    }
}
