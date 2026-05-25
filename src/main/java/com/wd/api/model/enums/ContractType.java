package com.wd.api.model.enums;

public enum ContractType {
    TURNKEY("Turnkey (Material + Labor)"),

    /**
     * @deprecated retained for backward-compat with existing projects;
     *             not offered for new projects.
     */
    @Deprecated
    LABOR_ONLY("Labor Only"),

    ITEM_RATE("Item Rate"),

    /**
     * @deprecated retained for backward-compat with existing projects;
     *             not offered for new projects.
     */
    @Deprecated
    COST_PLUS("Cost Plus (Cost + Margin)");

    private final String displayName;

    ContractType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns true only for contract types that Walldot currently offers
     * for new projects (TURNKEY and ITEM_RATE).
     * LABOR_ONLY and COST_PLUS are retained in this enum solely for
     * backward-compatibility when loading existing project rows.
     */
    public boolean isSupportedForNewProjects() {
        return this == TURNKEY || this == ITEM_RATE;
    }
}
