package com.wd.api.service;

import com.wd.api.model.Permission;
import com.wd.api.model.PortalRole;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.PermissionRepository;
import com.wd.api.repository.PortalRoleRepository;
import com.wd.api.repository.PortalUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Autowired
    private PortalRoleRepository portalRoleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private static final String ADMIN_ROLE_CODE = "ADMIN";

    /**
     * Get all permission names for a user
     * ADMIN users get ALL available permissions automatically
     */
    @SuppressWarnings("null")
    public List<String> getUserPermissions(Long userId) {
        PortalUser user = portalUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return getUserPermissions(user);
    }

    /**
     * Get all permission names for a user
     * ADMIN users get ALL available permissions automatically
     */
    public List<String> getUserPermissions(PortalUser user) {
        if (user.getRole() == null) {
            return Collections.emptyList();
        }

        // ADMIN bypass: User is admin, so they have implicit access.
        if (isAdmin(user)) {
            return Collections.emptyList();
        }

        // Return permissions from role
        Set<Permission> permissions = user.getRole().getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }

        return permissions.stream()
                .map(Permission::getName)
                .collect(Collectors.toList());
    }

    /**
     * Check if user has a specific permission
     * ADMIN always returns true
     */
    @SuppressWarnings("null")
    public boolean hasPermission(Long userId, String permission) {
        PortalUser user = portalUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return hasPermission(user, permission);
    }

    /**
     * Check if user has a specific permission
     * ADMIN always returns true
     */
    public boolean hasPermission(PortalUser user, String permission) {
        // ADMIN bypass
        if (isAdmin(user)) {
            return true;
        }

        List<String> permissions = getUserPermissions(user);
        return permissions.contains(permission);
    }

    /**
     * Check if user has ANY of the specified permissions
     * ADMIN always returns true
     */
    public boolean hasAnyPermission(PortalUser user, List<String> permissions) {
        // ADMIN bypass
        if (isAdmin(user)) {
            return true;
        }

        List<String> userPermissions = getUserPermissions(user);
        return permissions.stream()
                .anyMatch(userPermissions::contains);
    }

    /**
     * Check if user has ALL of the specified permissions
     * ADMIN always returns true
     */
    public boolean hasAllPermissions(PortalUser user, List<String> permissions) {
        // ADMIN bypass
        if (isAdmin(user)) {
            return true;
        }

        List<String> userPermissions = getUserPermissions(user);
        return userPermissions.containsAll(permissions);
    }

    /**
     * Check if user is an ADMIN
     */
    public boolean isAdmin(PortalUser user) {
        if (user.getRole() == null) {
            return false;
        }

        String roleCode = user.getRole().getCode();
        return "ADMIN".equalsIgnoreCase(roleCode) || "ROLE_ADMIN".equalsIgnoreCase(roleCode);
    }

    /**
     * Get ALL available permissions in the system
     */
    public List<String> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(Permission::getName)
                .collect(Collectors.toList());
    }

    /**
     * Get permissions for a specific role
     */
    @SuppressWarnings("null")
    public List<String> getRolePermissions(Long roleId) {
        PortalRole role = portalRoleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        // ADMIN role gets ALL permissions
        if (ADMIN_ROLE_CODE.equalsIgnoreCase(role.getCode())) {
            return getAllPermissions();
        }

        Set<Permission> permissions = role.getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }

        return permissions.stream()
                .map(Permission::getName)
                .collect(Collectors.toList());
    }

    /**
     * Helper method: Check if user can view a module
     */
    public boolean canView(PortalUser user, String module) {
        return hasPermission(user, module + "_VIEW");
    }

    /**
     * Helper method: Check if user can create in a module
     */
    public boolean canCreate(PortalUser user, String module) {
        return hasPermission(user, module + "_CREATE");
    }

    /**
     * Helper method: Check if user can edit in a module
     */
    public boolean canEdit(PortalUser user, String module) {
        return hasPermission(user, module + "_EDIT");
    }

    /**
     * Helper method: Check if user can delete in a module
     */
    public boolean canDelete(PortalUser user, String module) {
        return hasPermission(user, module + "_DELETE");
    }
}
