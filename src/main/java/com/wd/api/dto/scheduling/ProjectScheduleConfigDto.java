package com.wd.api.dto.scheduling;

public record ProjectScheduleConfigDto(
        Long projectId,
        Boolean sundayWorking,
        Short monsoonStartMonthDay,
        Short monsoonEndMonthDay,
        String districtCode) { }
