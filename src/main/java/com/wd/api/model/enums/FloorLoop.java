package com.wd.api.model.enums;

/**
 * Whether a WBS template task expands to one Task per floor (PER_FLOOR)
 * or to a single Task at phase level (NONE) at clone time.
 */
public enum FloorLoop {
    NONE,
    PER_FLOOR
}
