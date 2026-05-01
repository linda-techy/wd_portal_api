package com.wd.api.estimation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CustomisationChoiceDto(
        @NotNull UUID categoryId,
        @NotNull UUID optionId) {}
