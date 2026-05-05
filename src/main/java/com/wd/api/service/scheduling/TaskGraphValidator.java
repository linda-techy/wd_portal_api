package com.wd.api.service.scheduling;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * DFS cycle detection for the task-predecessor graph.
 *
 * <p>Pure-ish: takes a {@code Function<Long, List<Long>>} that returns the
 * direct predecessors of a given task id, so the validator is testable
 * without a database.
 */
public final class TaskGraphValidator {

    private TaskGraphValidator() { /* utility */ }

    /**
     * Throws if adding the edge {@code (successor <- predecessor)} would
     * introduce a cycle.
     *
     * <p>Algorithm: walk the predecessors-of graph starting at
     * {@code predecessor}. If {@code successor} is reachable, the new edge
     * would close a cycle.
     */
    public static void assertNoCycle(
            Long successorId,
            Long predecessorId,
            Function<Long, List<Long>> predecessorsOf) {
        if (successorId == null || predecessorId == null) {
            throw new IllegalArgumentException("successor and predecessor must be non-null");
        }
        if (successorId.equals(predecessorId)) {
            throw new CycleDetectedException(
                    "Self-loop: task " + successorId + " cannot depend on itself");
        }

        Set<Long> visited = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(predecessorId);
        while (!stack.isEmpty()) {
            Long current = stack.pop();
            if (!visited.add(current)) continue;
            if (current.equals(successorId)) {
                throw new CycleDetectedException(
                        "Cycle: predecessor " + predecessorId
                        + " transitively depends on successor " + successorId);
            }
            for (Long pred : predecessorsOf.apply(current)) {
                if (!visited.contains(pred)) stack.push(pred);
            }
        }
    }

    public static class CycleDetectedException extends RuntimeException {
        public CycleDetectedException(String message) { super(message); }
    }
}
