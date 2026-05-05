package com.wd.api.service.scheduling.dto;

import java.time.LocalDateTime;
import java.util.List;

public record WbsTemplateDto(
        Long id,
        String code,
        String projectType,
        String name,
        String description,
        Integer version,
        Boolean isActive,
        List<WbsTemplatePhaseDto> phases,
        LocalDateTime updatedAt
) {}
