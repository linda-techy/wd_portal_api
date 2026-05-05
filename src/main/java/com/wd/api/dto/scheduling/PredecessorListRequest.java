package com.wd.api.dto.scheduling;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PredecessorListRequest(@NotNull List<Entry> predecessors) {
    public record Entry(@NotNull Long predecessorId, Integer lagDays) {}
}
