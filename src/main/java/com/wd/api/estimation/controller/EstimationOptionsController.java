package com.wd.api.estimation.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.estimation.dto.EstimationOptionsResponse;
import com.wd.api.estimation.service.EstimationOptionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/estimation/options")
@PreAuthorize("isAuthenticated()")
public class EstimationOptionsController {

    private final EstimationOptionsService service;

    public EstimationOptionsController(EstimationOptionsService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<ApiResponse<EstimationOptionsResponse>> get(
            @RequestParam(required = false) UUID packageId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.listOptions(packageId)));
    }
}
