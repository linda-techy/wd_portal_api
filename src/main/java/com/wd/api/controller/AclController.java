package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.model.Permission;
import com.wd.api.model.PortalRole;
import com.wd.api.model.enums.PortalRoleCode;
import com.wd.api.repository.PermissionRepository;
import com.wd.api.repository.PortalRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ACL (Access Control List) Controller
 *
 * Manages portal role permissions — read roles, view/update their permission sets,
 * and retrieve predefined permission templates for common construction roles.
 *
 * All endpoints require PORTAL_USER_VIEW (read) or PORTAL_USER_EDIT (write).
 */
@RestController
@RequestMapping("/acl")
public class AclController {

    private static final Logger log = LoggerFactory.getLogger(AclController.class);

    @Autowired
    private PortalRoleRepository portalRoleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Inner DTOs (Java 16+ records)
    // ─────────────────────────────────────────────────────────────────────────

    public record PermissionDto(Long id, String name, String description) {}

    public record ModulePermissionGroup(String module, String displayName, List<PermissionDto> permissions) {}

    public record RoleSummaryDto(Long id, String name, String code, String description, int permissionCount) {}

    public record RoleDetailDto(Long id, String name, String code, String description, List<String> permissionNames) {}

    public record UpdateRolePermissionsRequest(List<Long> permissionIds) {}

    public record RoleTemplateDto(String roleCode, String displayName, String description, List<String> permissionNames) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /acl/roles
     * Returns all portal roles with permission count summary.
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('PORTAL_USER_VIEW')")
    public ResponseEntity<ApiResponse<List<RoleSummaryDto>>> getAllRoles() {
        try {
            List<PortalRole> roles = portalRoleRepository.findAll();
            List<RoleSummaryDto> result = roles.stream()
                .sorted(Comparator.comparing(PortalRole::getName))
                .map(r -> new RoleSummaryDto(
                    r.getId(),
                    r.getName(),
                    r.getCode() != null ? r.getCode() : "",
                    r.getDescription(),
                    r.getPermissions() != null ? r.getPermissions().size() : 0
                ))
                .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success("Roles retrieved", result));
        } catch (Exception e) {
            log.error("Error fetching ACL roles", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to load roles"));
        }
    }

    /**
     * GET /acl/roles/{id}
     * Returns a single role with its full permission name list.
     */
    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('PORTAL_USER_VIEW')")
    public ResponseEntity<ApiResponse<RoleDetailDto>> getRoleDetail(@PathVariable Long id) {
        try {
            return portalRoleRepository.findById(id)
                .map(role -> {
                    boolean isAdmin = PortalRoleCode.isAdmin(role.getCode());
                    List<String> permNames;
                    if (isAdmin) {
                        // ADMIN gets all permissions
                        permNames = permissionRepository.findAll().stream()
                            .map(Permission::getName)
                            .sorted()
                            .collect(Collectors.toList());
                    } else {
                        permNames = role.getPermissions() != null
                            ? role.getPermissions().stream()
                                .map(Permission::getName)
                                .sorted()
                                .collect(Collectors.toList())
                            : Collections.emptyList();
                    }
                    RoleDetailDto dto = new RoleDetailDto(
                        role.getId(), role.getName(), role.getCode(), role.getDescription(), permNames);
                    return ResponseEntity.ok(ApiResponse.success("Role detail retrieved", dto));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("Role not found")));
        } catch (Exception e) {
            log.error("Error fetching role detail id={}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to load role detail"));
        }
    }

    /**
     * PUT /acl/roles/{id}/permissions
     * Bulk-replaces all permissions for a role.
     * ADMIN role cannot be edited — it always has all permissions.
     */
    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('PORTAL_USER_EDIT')")
    public ResponseEntity<ApiResponse<RoleDetailDto>> updateRolePermissions(
            @PathVariable Long id,
            @RequestBody UpdateRolePermissionsRequest request) {
        try {
            Optional<PortalRole> roleOpt = portalRoleRepository.findById(id);
            if (roleOpt.isEmpty()) {
                return ResponseEntity.status(404).body(ApiResponse.error("Role not found"));
            }

            PortalRole role = roleOpt.get();
            if (PortalRoleCode.isAdmin(role.getCode())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Cannot edit ADMIN role permissions — ADMIN always has all permissions"));
            }

            List<Long> ids = request.permissionIds() != null ? request.permissionIds() : Collections.emptyList();
            List<Permission> permissions = permissionRepository.findAllById(ids);
            role.setPermissions(new HashSet<>(permissions));
            portalRoleRepository.save(role);

            List<String> permNames = permissions.stream()
                .map(Permission::getName)
                .sorted()
                .collect(Collectors.toList());
            RoleDetailDto dto = new RoleDetailDto(
                role.getId(), role.getName(), role.getCode(), role.getDescription(), permNames);

            log.info("Updated permissions for role {} ({}): {} permissions", role.getName(), role.getCode(), permissions.size());
            return ResponseEntity.ok(ApiResponse.success("Role permissions updated successfully", dto));
        } catch (Exception e) {
            log.error("Error updating permissions for role id={}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to update permissions"));
        }
    }

    /**
     * GET /acl/permissions
     * Returns all system permissions grouped by module.
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PORTAL_USER_VIEW')")
    public ResponseEntity<ApiResponse<List<ModulePermissionGroup>>> getAllPermissionsGrouped() {
        try {
            List<Permission> allPerms = permissionRepository.findAll();

            // Group by extracted module prefix
            Map<String, List<PermissionDto>> grouped = new LinkedHashMap<>();
            for (Permission p : allPerms) {
                String module = extractModule(p.getName());
                grouped.computeIfAbsent(module, k -> new ArrayList<>())
                       .add(new PermissionDto(p.getId(), p.getName(), p.getDescription()));
            }
            // Sort permissions within each module
            grouped.values().forEach(list -> list.sort(Comparator.comparing(PermissionDto::name)));

            // Build sorted module groups
            List<ModulePermissionGroup> result = grouped.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> moduleOrder(e.getKey())))
                .map(e -> new ModulePermissionGroup(e.getKey(), toDisplayName(e.getKey()), e.getValue()))
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Permissions retrieved", result));
        } catch (Exception e) {
            log.error("Error fetching ACL permissions", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to load permissions"));
        }
    }

    /**
     * GET /acl/role-templates
     * Returns predefined permission sets for common construction roles.
     * Only includes permission names that actually exist in the database.
     */
    @GetMapping("/role-templates")
    @PreAuthorize("hasAuthority('PORTAL_USER_VIEW')")
    public ResponseEntity<ApiResponse<List<RoleTemplateDto>>> getRoleTemplates() {
        try {
            Set<String> existingPerms = permissionRepository.findAll().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

            List<RoleTemplateDto> templates = buildTemplates().stream()
                .map(t -> new RoleTemplateDto(
                    t.roleCode(),
                    t.displayName(),
                    t.description(),
                    t.permissionNames().stream()
                        .filter(existingPerms::contains)
                        .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Role templates retrieved", templates));
        } catch (Exception e) {
            log.error("Error fetching role templates", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to load templates"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Extract module key from permission name (strip trailing _VERB). */
    private String extractModule(String permissionName) {
        String[] verbs = {"_VIEW", "_CREATE", "_EDIT", "_DELETE", "_APPROVE", "_EXPORT", "_FILTER"};
        for (String verb : verbs) {
            if (permissionName.endsWith(verb)) {
                return permissionName.substring(0, permissionName.length() - verb.length());
            }
        }
        return permissionName;
    }

    private static final List<String> MODULE_ORDER = List.of(
        "DASHBOARD", "LEAD", "CUSTOMER", "PROJECT", "TASK", "PORTAL_USER",
        "SITE_REPORT", "GALLERY", "BOQ", "FINANCE", "PAYMENT",
        "PROCUREMENT", "INVENTORY", "LABOUR", "ATTENDANCE",
        "QC", "OBSERVATION", "SNAG", "QUERY", "REPORT", "NOTIFICATION"
    );

    private int moduleOrder(String module) {
        int idx = MODULE_ORDER.indexOf(module);
        return idx >= 0 ? idx : 999;
    }

    /** Human-readable module display names. */
    private String toDisplayName(String module) {
        return switch (module) {
            case "DASHBOARD"   -> "Dashboard";
            case "LEAD"        -> "Leads";
            case "PROJECT"     -> "Projects";
            case "TASK"        -> "Tasks";
            case "CUSTOMER"    -> "Customers";
            case "PORTAL_USER" -> "Portal Users";
            case "REPORT"      -> "Reports";
            case "FINANCE"     -> "Finance";
            case "PAYMENT"     -> "Payments";
            case "BOQ"         -> "BOQ (Bill of Quantities)";
            case "SITE_REPORT" -> "Site Reports";
            case "GALLERY"     -> "Gallery";
            case "LABOUR"      -> "Labour";
            case "PROCUREMENT" -> "Procurement";
            case "INVENTORY"   -> "Inventory";
            case "QC"          -> "Quality Checks";
            case "OBSERVATION" -> "Observations";
            case "SNAG"        -> "Snags";
            case "QUERY"       -> "Queries";
            case "ATTENDANCE"  -> "Attendance";
            case "NOTIFICATION"-> "Notifications";
            default            -> module.replace("_", " ");
        };
    }

    /** Predefined permission templates for common construction roles. */
    private List<RoleTemplateDto> buildTemplates() {
        return List.of(
            new RoleTemplateDto("PROJECT_MANAGER", "Project Manager",
                "Full project management — projects, tasks, BOQ, site reports, labour, procurement",
                List.of(
                    "DASHBOARD_VIEW", "DASHBOARD_FILTER",
                    "PROJECT_VIEW", "PROJECT_CREATE", "PROJECT_EDIT",
                    "TASK_VIEW", "TASK_CREATE", "TASK_EDIT", "TASK_DELETE",
                    "CUSTOMER_VIEW",
                    "BOQ_VIEW", "BOQ_CREATE", "BOQ_EDIT", "BOQ_APPROVE",
                    "SITE_REPORT_VIEW", "SITE_REPORT_CREATE", "SITE_REPORT_EDIT",
                    "GALLERY_VIEW", "GALLERY_CREATE",
                    "LABOUR_VIEW", "LABOUR_CREATE", "LABOUR_EDIT",
                    "PROCUREMENT_VIEW", "PROCUREMENT_CREATE", "PROCUREMENT_EDIT", "PROCUREMENT_APPROVE",
                    "INVENTORY_VIEW", "INVENTORY_CREATE", "INVENTORY_EDIT",
                    "QC_VIEW", "QC_CREATE", "QC_EDIT",
                    "OBSERVATION_VIEW", "OBSERVATION_CREATE", "OBSERVATION_EDIT",
                    "SNAG_VIEW", "SNAG_CREATE", "SNAG_EDIT",
                    "QUERY_VIEW", "QUERY_CREATE", "QUERY_EDIT",
                    "ATTENDANCE_VIEW", "ATTENDANCE_CREATE", "ATTENDANCE_EDIT",
                    "PAYMENT_VIEW", "PAYMENT_CREATE",
                    "REPORT_VIEW",
                    "NOTIFICATION_VIEW"
                )),
            new RoleTemplateDto("SITE_ENGINEER", "Site Engineer",
                "Field operations — site reports, tasks, labour attendance, gallery, QC",
                List.of(
                    "DASHBOARD_VIEW",
                    "PROJECT_VIEW",
                    "TASK_VIEW", "TASK_CREATE", "TASK_EDIT",
                    "BOQ_VIEW",
                    "SITE_REPORT_VIEW", "SITE_REPORT_CREATE", "SITE_REPORT_EDIT",
                    "GALLERY_VIEW", "GALLERY_CREATE",
                    "LABOUR_VIEW", "LABOUR_CREATE", "LABOUR_EDIT",
                    "QC_VIEW", "QC_CREATE", "QC_EDIT",
                    "OBSERVATION_VIEW", "OBSERVATION_CREATE", "OBSERVATION_EDIT",
                    "INVENTORY_VIEW",
                    "ATTENDANCE_VIEW", "ATTENDANCE_CREATE", "ATTENDANCE_EDIT",
                    "NOTIFICATION_VIEW"
                )),
            new RoleTemplateDto("FINANCE_OFFICER", "Finance Officer",
                "Finance, payments, invoices, and financial reporting",
                List.of(
                    "DASHBOARD_VIEW", "DASHBOARD_FILTER",
                    "FINANCE_VIEW", "FINANCE_CREATE", "FINANCE_EDIT",
                    "PAYMENT_VIEW", "PAYMENT_CREATE", "PAYMENT_EDIT", "PAYMENT_APPROVE",
                    "BOQ_VIEW",
                    "REPORT_VIEW", "REPORT_EXPORT",
                    "PROJECT_VIEW",
                    "CUSTOMER_VIEW",
                    "NOTIFICATION_VIEW"
                )),
            new RoleTemplateDto("PROCUREMENT_OFFICER", "Procurement Officer",
                "Procurement, purchase orders, and inventory management",
                List.of(
                    "DASHBOARD_VIEW",
                    "PROCUREMENT_VIEW", "PROCUREMENT_CREATE", "PROCUREMENT_EDIT", "PROCUREMENT_APPROVE",
                    "INVENTORY_VIEW", "INVENTORY_CREATE", "INVENTORY_EDIT", "INVENTORY_DELETE",
                    "PROJECT_VIEW",
                    "BOQ_VIEW",
                    "REPORT_VIEW",
                    "NOTIFICATION_VIEW"
                )),
            new RoleTemplateDto("SALES", "Sales / CRM Executive",
                "Lead management, customer handling, and follow-ups",
                List.of(
                    "DASHBOARD_VIEW", "DASHBOARD_FILTER",
                    "LEAD_VIEW", "LEAD_CREATE", "LEAD_EDIT", "LEAD_DELETE", "LEAD_EXPORT",
                    "CUSTOMER_VIEW", "CUSTOMER_CREATE", "CUSTOMER_EDIT",
                    "PROJECT_VIEW",
                    "REPORT_VIEW",
                    "NOTIFICATION_VIEW"
                )),
            new RoleTemplateDto("SITE_SUPERVISOR", "Site Supervisor",
                "On-site supervision — labour, attendance, tasks, and observations",
                List.of(
                    "DASHBOARD_VIEW",
                    "PROJECT_VIEW",
                    "TASK_VIEW", "TASK_CREATE", "TASK_EDIT",
                    "SITE_REPORT_VIEW", "SITE_REPORT_CREATE",
                    "GALLERY_VIEW", "GALLERY_CREATE",
                    "LABOUR_VIEW", "LABOUR_CREATE", "LABOUR_EDIT",
                    "ATTENDANCE_VIEW", "ATTENDANCE_CREATE", "ATTENDANCE_EDIT",
                    "OBSERVATION_VIEW", "OBSERVATION_CREATE",
                    "QC_VIEW",
                    "NOTIFICATION_VIEW"
                )),
            new RoleTemplateDto("QUALITY_SAFETY", "Quality & Safety Officer",
                "Quality checks, observations, snags, and site reports",
                List.of(
                    "DASHBOARD_VIEW",
                    "PROJECT_VIEW",
                    "QC_VIEW", "QC_CREATE", "QC_EDIT",
                    "OBSERVATION_VIEW", "OBSERVATION_CREATE", "OBSERVATION_EDIT",
                    "SNAG_VIEW", "SNAG_CREATE", "SNAG_EDIT",
                    "SITE_REPORT_VIEW", "SITE_REPORT_CREATE", "SITE_REPORT_EDIT",
                    "GALLERY_VIEW",
                    "REPORT_VIEW",
                    "NOTIFICATION_VIEW"
                )),
            new RoleTemplateDto("ESTIMATOR", "Estimator / Quantity Surveyor",
                "BOQ estimation, cost analysis, and project finance visibility",
                List.of(
                    "DASHBOARD_VIEW",
                    "PROJECT_VIEW",
                    "BOQ_VIEW", "BOQ_CREATE", "BOQ_EDIT",
                    "FINANCE_VIEW",
                    "PAYMENT_VIEW",
                    "REPORT_VIEW", "REPORT_EXPORT",
                    "NOTIFICATION_VIEW"
                )),
            new RoleTemplateDto("INTERN", "Intern / Trainee",
                "Read-only access for learning and observation",
                List.of(
                    "DASHBOARD_VIEW",
                    "PROJECT_VIEW",
                    "TASK_VIEW",
                    "SITE_REPORT_VIEW",
                    "GALLERY_VIEW",
                    "NOTIFICATION_VIEW"
                ))
        );
    }
}
