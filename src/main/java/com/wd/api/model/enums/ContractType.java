package com.wd.api.model.enums;

public enum ContractType {
    TURNKEY("Turnkey (Material + Labor)"),
    LABOR_ONLY("Labor Only"),
    ITEM_RATE("Item Rate"),
    COST_PLUS("Cost Plus (Cost + Margin)");

    private final String displayName;

    ContractType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
