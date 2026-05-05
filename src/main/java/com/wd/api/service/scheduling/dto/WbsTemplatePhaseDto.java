package com.wd.api.service.scheduling.dto;

import java.util.List;

public record WbsTemplatePhaseDto(
        Long id,
        Integer sequence,
        String name,
        String roleHint,
        Boolean monsoonSensitive,
        List<WbsTemplateTaskDto> tasks
) {}
