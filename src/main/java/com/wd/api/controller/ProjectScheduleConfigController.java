package com.wd.api.controller;

import com.wd.api.dto.scheduling.HolidayOverrideRequest;
import com.wd.api.dto.scheduling.ProjectScheduleConfigDto;
import com.wd.api.model.scheduling.ProjectHolidayOverride;
import com.wd.api.service.scheduling.ProjectScheduleConfigService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
@PreAuthorize("isAuthenticated()")
public class ProjectScheduleConfigController {

    private final ProjectScheduleConfigService service;

    public ProjectScheduleConfigController(ProjectScheduleConfigService service) {
        this.service = service;
    }

    @GetMapping("/schedule-config")
    @PreAuthorize("hasAnyAuthority('HOLIDAY_VIEW','PROJECT_SCHEDULE_CONFIG_EDIT')")
    public ProjectScheduleConfigDto get(@PathVariable Long projectId) {
        return service.get(projectId);
    }

    @PutMapping("/schedule-config")
    @PreAuthorize("hasAuthority('PROJECT_SCHEDULE_CONFIG_EDIT')")
    public ProjectScheduleConfigDto put(@PathVariable Long projectId,
                                        @Valid @RequestBody ProjectScheduleConfigDto dto) {
        return service.upsert(projectId, dto);
    }

    @PostMapping("/holiday-overrides")
    @PreAuthorize("hasAuthority('PROJECT_HOLIDAY_OVERRIDE')")
    public ResponseEntity<Long> postOverride(@PathVariable Long projectId,
                                             @Valid @RequestBody HolidayOverrideRequest req) {
        ProjectHolidayOverride saved = service.addOverride(projectId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.getId());
    }

    @DeleteMapping("/holiday-overrides/{overrideId}")
    @PreAuthorize("hasAuthority('PROJECT_HOLIDAY_OVERRIDE')")
    public ResponseEntity<Void> deleteOverride(@PathVariable Long projectId,
                                               @PathVariable Long overrideId) {
        service.deleteOverride(projectId, overrideId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/holiday-overrides")
    @PreAuthorize("hasAnyAuthority('HOLIDAY_VIEW','PROJECT_HOLIDAY_OVERRIDE')")
    public List<ProjectHolidayOverride> listOverrides(@PathVariable Long projectId) {
        return service.listOverrides(projectId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadInput(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
