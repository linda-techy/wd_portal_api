package com.wd.api.controller.scheduling;

import com.wd.api.dto.scheduling.CpmResultDto;
import com.wd.api.service.scheduling.CpmService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/projects/{id}/cpm — single round-trip snapshot of the CPM
 * result for a project. Used by the Flutter Gantt screen (PR3) and any
 * client that wants es/ef/ls/lf/float/critical without iterating tasks.
 *
 * <p>Read-only: does NOT trigger a recompute. Recompute is wired into
 * mutation paths (TaskPredecessorService, GanttService.updateTaskSchedule).
 */
@RestController
@RequestMapping("/api/projects/{projectId}")
public class CpmController {

    private final CpmService cpm;

    public CpmController(CpmService cpm) {
        this.cpm = cpm;
    }

    @GetMapping("/cpm")
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    public ResponseEntity<CpmResultDto> getCpmResult(@PathVariable Long projectId) {
        return ResponseEntity.ok(cpm.read(projectId));
    }
}
