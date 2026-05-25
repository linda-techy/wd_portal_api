package com.wd.api.model.enums;

/**
 * Dependency relationship type between two tasks in the CPM network.
 *
 * <ul>
 *   <li>FS — Finish-to-Start  (default): successor cannot start until predecessor finishes.</li>
 *   <li>SS — Start-to-Start:  successor cannot start until predecessor starts.</li>
 *   <li>FF — Finish-to-Finish: successor cannot finish until predecessor finishes.</li>
 *   <li>SF — Start-to-Finish:  successor cannot finish until predecessor starts.</li>
 * </ul>
 *
 * All four types support an optional lag (working days). Stored as VARCHAR(2)
 * in the database; enforced by a CHECK constraint (see V160).
 */
public enum DependencyType {
    FS,
    SS,
    FF,
    SF;

    /** Parse a nullable string to a DependencyType, defaulting to FS if blank/null. */
    public static DependencyType fromString(String s) {
        if (s == null || s.isBlank()) return FS;
        return DependencyType.valueOf(s.toUpperCase());
    }
}
