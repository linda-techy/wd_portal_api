package com.wd.api.dto.scheduling;

import com.wd.api.model.scheduling.HolidayOverrideAction;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record HolidayOverrideRequest(
        @NotNull HolidayOverrideAction action,
        @NotNull LocalDate overrideDate,
        Long holidayId,
        String overrideName) { }
