package com.wd.api.model.enums;

/**
 * Cost-split category for a Variation Order.
 * Drives the default advance/progress/completion payment percentages
 * when a VOPaymentSchedule is auto-created on VO approval.
 */
public enum VOCategory {
    /** Supply-dominated work. Default split: Advance 40% | Progress 40% | Completion 20% */
    MATERIAL_HEAVY,
    /** Labour-dominated work. Default split: Advance 20% | Progress 60% | Completion 20% */
    LABOUR_HEAVY,
    /** Mixed supply and labour. Default split: Advance 30% | Progress 50% | Completion 20% */
    MIXED,
    /** User-defined split. advance+progress+completion must still equal 100. */
    CUSTOM;

    /** Default advance % for auto-schedule creation. */
    public int defaultAdvancePct() {
        return switch (this) {
            case MATERIAL_HEAVY -> 40;
            case LABOUR_HEAVY   -> 20;
            case MIXED          -> 30;
            case CUSTOM         -> 30;
        };
    }

    public int defaultProgressPct() {
        return switch (this) {
            case MATERIAL_HEAVY -> 40;
            case LABOUR_HEAVY   -> 60;
            case MIXED          -> 50;
            case CUSTOM         -> 50;
        };
    }

    public int defaultCompletionPct() {
        return 100 - defaultAdvancePct() - defaultProgressPct();
    }
}
