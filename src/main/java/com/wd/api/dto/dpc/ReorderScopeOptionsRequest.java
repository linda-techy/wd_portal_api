package com.wd.api.dto.dpc;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request body for reordering options under one scope template.
 *
 * <p>{@code orderedOptionIds} must contain every active option id of the
 * scope, in the desired final order. The service rejects partial lists.
 */
public record ReorderScopeOptionsRequest(
        @NotNull @NotEmpty List<Long> orderedOptionIds
) {
}
