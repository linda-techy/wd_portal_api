package com.wd.api.estimation.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.estimation.dto.admin.MarketIndexCreateRequest;
import com.wd.api.estimation.dto.admin.MarketIndexResponse;
import com.wd.api.estimation.service.admin.MarketIndexAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/estimation/market-index")
@PreAuthorize("isAuthenticated()")
public class MarketIndexAdminController {

    private final MarketIndexAdminService service;

    public MarketIndexAdminController(MarketIndexAdminService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ESTIMATION_SETTINGS_VIEW')")
    public ResponseEntity<ApiResponse<List<MarketIndexResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.list()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ESTIMATION_SETTINGS_VIEW')")
    public ResponseEntity<ApiResponse<MarketIndexResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.get(id)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('ESTIMATION_SETTINGS_VIEW')")
    public ResponseEntity<ApiResponse<MarketIndexResponse>> getActive() {
        Optional<MarketIndexResponse> active = service.getActive();
        return active
                .map(s -> ResponseEntity.ok(ApiResponse.success("OK", s)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<MarketIndexResponse>error("No active market index snapshot")));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ESTIMATION_MARKET_INDEX_PUBLISH')")
    public ResponseEntity<ApiResponse<MarketIndexResponse>> publish(
            @Valid @RequestBody MarketIndexCreateRequest req) {
        MarketIndexResponse created = service.createSnapshot(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Market index snapshot published (composite computed server-side)", created));
    }
}
