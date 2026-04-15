package com.wd.api.model.enums;

public enum ChangeOrderType {
    SCOPE_ADDITION,
    SCOPE_REDUCTION,
    UNFORESEEN_VARIATION,
    QUANTITY_VARIATION_INC,
    QUANTITY_VARIATION_DEC,
    RATE_VARIATION_INC,
    RATE_VARIATION_DEC;

    /** Returns true if this CO type results in a reduction (credit note). */
    public boolean isReduction() {
        return this == SCOPE_REDUCTION
                || this == QUANTITY_VARIATION_DEC
                || this == RATE_VARIATION_DEC;
    }

    /** Returns true if this CO type results in an addition (CO invoice). */
    public boolean isAddition() {
        return this == SCOPE_ADDITION
                || this == QUANTITY_VARIATION_INC
                || this == RATE_VARIATION_INC
                || this == UNFORESEEN_VARIATION;
    }
}
