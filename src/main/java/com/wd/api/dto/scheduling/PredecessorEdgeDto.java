package com.wd.api.dto.scheduling;

/**
 * Single predecessor edge as returned by GET /api/tasks/{taskId}/predecessors.
 * Includes the predecessor task's title so the Flutter UI can render the chip
 * without a second round-trip per row.
 */
public record PredecessorEdgeDto(
        Long id,
        Long successorId,
        Long predecessorId,
        String predecessorTitle,
        Integer lagDays,
        String depType
) {}
