package com.wd.api.service.scheduling.dto;

import com.wd.api.model.enums.FloorLoop;

import java.math.BigDecimal;
import java.util.List;

public record WbsTemplateTaskDto(
        Long id,
        Integer sequence,
        String name,
        String roleHint,
        Integer durationDays,
        Integer weightFactor,
        Boolean monsoonSensitive,
        Boolean isPaymentMilestone,
        FloorLoop floorLoop,
        BigDecimal optionalCost,
        List<WbsTemplateTaskPredecessorDto> predecessors
) {}
