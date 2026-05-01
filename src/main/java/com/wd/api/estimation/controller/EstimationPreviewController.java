package com.wd.api.estimation.controller;

import com.wd.api.estimation.dto.CalculatePreviewRequest;
import com.wd.api.estimation.dto.CalculatePreviewResponse;
import com.wd.api.estimation.service.EstimationPreviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estimations")
public class EstimationPreviewController {

    private final EstimationPreviewService service;

    public EstimationPreviewController(EstimationPreviewService service) {
        this.service = service;
    }

    @PostMapping("/calculate")
    public CalculatePreviewResponse calculate(@Valid @RequestBody CalculatePreviewRequest request) {
        return service.preview(request);
    }
}
