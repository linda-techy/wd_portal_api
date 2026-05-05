package com.wd.api.service.scheduling;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskGraphValidatorTest {

    // Helper: turns a Map<successor -> List<predecessor>> into the lookup
    // function the validator expects.
    private static Function<Long, List<Long>> graph(Map<Long, List<Long>> g) {
        return id -> g.getOrDefault(id, List.of());
    }

    @Test
    void noCycle_acceptsLinearChain() {
        // 1 -> 2 -> 3. Adding 4 as predecessor of 3 is fine.
        Map<Long, List<Long>> g = Map.of(2L, List.of(1L), 3L, List.of(2L));
        assertThatCode(() -> TaskGraphValidator.assertNoCycle(3L, 4L, graph(g)))
                .doesNotThrowAnyException();
    }

    @Test
    void noCycle_rejectsDirectSelfLoop() {
        assertThatThrownBy(() ->
                TaskGraphValidator.assertNoCycle(5L, 5L, graph(Map.of())))
                .isInstanceOf(TaskGraphValidator.CycleDetectedException.class);
    }

    @Test
    void noCycle_rejects_AtoBtoCtoA() {
        // existing edges: B->A, C->B. Adding A as successor with C as predecessor would
        // close the cycle A->C->B->A. Validator is asked: would predecessor=C
        // already reach successor=A?
        Map<Long, List<Long>> g = Map.of(
                1L, List.of(),       // A
                2L, List.of(1L),     // B's predecessors = {A}
                3L, List.of(2L));    // C's predecessors = {B}
        assertThatThrownBy(() ->
                TaskGraphValidator.assertNoCycle(1L, 3L, graph(g)))
                .isInstanceOf(TaskGraphValidator.CycleDetectedException.class);
    }

    @Test
    void noCycle_rejects_indirectFiveNode() {
        // 1 <- 2 <- 3 <- 4 <- 5. Adding edge 5->1 (i.e., successor=5, predecessor=1)
        // closes nothing. But adding edge 1->5 (successor=1, predecessor=5) closes
        // 1 -> 5 -> 4 -> 3 -> 2 -> 1.
        Map<Long, List<Long>> g = Map.of(
                2L, List.of(1L), 3L, List.of(2L), 4L, List.of(3L), 5L, List.of(4L));
        assertThatThrownBy(() ->
                TaskGraphValidator.assertNoCycle(1L, 5L, graph(g)))
                .isInstanceOf(TaskGraphValidator.CycleDetectedException.class);
    }

    @Test
    void noCycle_acceptsDiamond_noCycle() {
        // 1 -> 2, 1 -> 3, 2 -> 4, 3 -> 4. No cycle. Adding 5 -> 4 fine.
        Map<Long, List<Long>> g = Map.of(
                2L, List.of(1L), 3L, List.of(1L),
                4L, List.of(2L, 3L));
        assertThatCode(() -> TaskGraphValidator.assertNoCycle(4L, 5L, graph(g)))
                .doesNotThrowAnyException();
    }
}
