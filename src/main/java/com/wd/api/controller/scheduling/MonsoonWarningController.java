package com.wd.api.controller.scheduling;

import com.wd.api.service.scheduling.MonsoonWarningService;
import com.wd.api.service.scheduling.dto.MonsoonWarning;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/schedule")
@PreAuthorize("isAuthenticated()")
public class MonsoonWarningController {

    private final MonsoonWarningService warnings;

    public MonsoonWarningController(MonsoonWarningService warnings) {
        this.warnings = warnings;
    }

    @GetMapping("/warnings")
    @PreAuthorize("hasAuthority('MONSOON_WARNING_VIEW')")
    public List<MonsoonWarning> list(@PathVariable Long projectId) {
        return warnings.warningsFor(projectId);
    }
}
