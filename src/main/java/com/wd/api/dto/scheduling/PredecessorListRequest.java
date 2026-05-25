package com.wd.api.dto.scheduling;

import com.wd.api.model.enums.DependencyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PredecessorListRequest(@NotNull @Valid List<Entry> predecessors) {
    /** {@code depType} is optional; omitting it defaults to FS. */
    public record Entry(@NotNull Long predecessorId, Integer lagDays, DependencyType depType) {}
}
