package com.wd.api.dto.scheduling;

import com.wd.api.model.scheduling.HolidayRecurrenceType;
import com.wd.api.model.scheduling.HolidayScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record HolidayDto(
        Long id,
        String code,
        @NotBlank String name,
        @NotNull LocalDate date,
        @NotNull HolidayScope scope,
        String scopeRef,
        @NotNull HolidayRecurrenceType recurrenceType,
        Boolean active) { }
