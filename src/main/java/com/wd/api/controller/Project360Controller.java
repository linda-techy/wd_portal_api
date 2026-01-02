package com.wd.api.controller;

import com.wd.api.dto.ProjectSummaryDTO;
import com.wd.api.service.ProjectAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/360")
@RequiredArgsConstructor
public class Project360Controller {

    private final ProjectAggregationService aggregationService;

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectSummaryDTO> getProjectSummary(@PathVariable Long projectId) {
        return ResponseEntity.ok(aggregationService.getProjectSummary(projectId));
    }
}
