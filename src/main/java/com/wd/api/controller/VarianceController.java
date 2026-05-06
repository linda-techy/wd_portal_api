package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.service.scheduling.NoBaselineException;
import com.wd.api.service.scheduling.VarianceService;
import com.wd.api.service.scheduling.dto.VarianceRowDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/variance")
public class VarianceController {

    private final VarianceService service;

    public VarianceController(VarianceService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    public ResponseEntity<ApiResponse<List<VarianceRowDto>>> get(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success("Variance retrieved", service.computeFor(projectId)));
    }

    @ExceptionHandler(NoBaselineException.class)
    public ResponseEntity<ApiResponse<Void>> onNoBaseline(NoBaselineException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
    }
}
