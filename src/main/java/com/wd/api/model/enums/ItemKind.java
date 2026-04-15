package com.wd.api.model.enums;

public enum ItemKind {
    /** Always included in the contract; quantity must be > 0 */
    BASE,
    /** Charged extra if selected; quantity must be > 0 */
    ADDON,
    /** Customer may choose; quantity = 0 permitted */
    OPTIONAL,
    /** Explicitly out of scope; listed for transparency; quantity = 0 permitted */
    EXCLUSION
}
