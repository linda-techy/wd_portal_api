package com.wd.api.model.enums;

/**
 * Constants for portal role codes stored in the database.
 * Use these instead of hardcoded strings like "ADMIN".
 */
public final class PortalRoleCode {
    private PortalRoleCode() {}

    public static final String ADMIN = "ADMIN";
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String PROJECT_MANAGER = "PROJECT_MANAGER";
    public static final String SITE_ENGINEER = "SITE_ENGINEER";
    public static final String FINANCE_OFFICER = "FINANCE_OFFICER";
    public static final String ARCHITECT = "ARCHITECT";
    public static final String INTERIOR_DESIGNER = "INTERIOR_DESIGNER";
    public static final String PROCUREMENT_OFFICER = "PROCUREMENT_OFFICER";

    // S1 PR1 additions (DB seed lands in PR2)
    public static final String SCHEDULER = "SCHEDULER";
    public static final String QUANTITY_SURVEYOR = "QUANTITY_SURVEYOR";
    public static final String MANAGEMENT = "MANAGEMENT";

    public static boolean isAdmin(String roleCode) {
        return ADMIN.equalsIgnoreCase(roleCode) || SUPER_ADMIN.equalsIgnoreCase(roleCode);
    }
}
