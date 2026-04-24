package com.wd.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TemplateApplyRequest(@NotBlank String templateCode) {}
