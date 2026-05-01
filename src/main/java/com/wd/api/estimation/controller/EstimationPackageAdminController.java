package com.wd.api.estimation.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.estimation.dto.admin.PackageAdminCreateRequest;
import com.wd.api.estimation.dto.admin.PackageAdminResponse;
import com.wd.api.estimation.dto.admin.PackageAdminUpdateRequest;
import com.wd.api.estimation.service.admin.EstimationPackageAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/estimation/packages")
@PreAuthorize("isAuthenticated()")
public class EstimationPackageAdminController {

    private final EstimationPackageAdminService service;

    public EstimationPackageAdminController(EstimationPackageAdminService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ESTIMATION_SETTINGS_VIEW')")
    public ResponseEntity<ApiResponse<List<PackageAdminResponse>>> list(
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
        List<PackageAdminResponse> all = service.list(includeInactive);
        return ResponseEntity.ok(ApiResponse.success("OK", all));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ESTIMATION_SETTINGS_VIEW')")
    public ResponseEntity<ApiResponse<PackageAdminResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ESTIMATION_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<PackageAdminResponse>> create(
            @Valid @RequestBody PackageAdminCreateRequest req) {
        PackageAdminResponse created = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Package created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ESTIMATION_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<PackageAdminResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody PackageAdminUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Package updated", service.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ESTIMATION_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.softDelete(id);
        return ResponseEntity.ok(ApiResponse.success("Package deleted"));
    }
}
