package com.wd.api.estimation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddOnRefDto(@NotNull UUID id) {}
