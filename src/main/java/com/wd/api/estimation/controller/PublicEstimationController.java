package com.wd.api.estimation.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.estimation.dto.PublicEstimationResponse;
import com.wd.api.estimation.service.LeadEstimationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Unauthenticated endpoint — whitelisted in SecurityConfig via /public/**.
 * The UUID token in the path acts as the bearer credential.
 */
@RestController
@RequestMapping("/public/estimations")
public class PublicEstimationController {

    private final LeadEstimationService service;

    public PublicEstimationController(LeadEstimationService service) {
        this.service = service;
    }

    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<PublicEstimationResponse>> get(@PathVariable UUID token) {
        return service.getByPublicToken(token)
                .map(r -> ResponseEntity.ok(ApiResponse.success("OK", r)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<PublicEstimationResponse>error("Estimation not found")));
    }
}
