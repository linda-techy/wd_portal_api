package com.wd.api.service;

import com.wd.api.model.Permission;
import com.wd.api.model.Role;
import com.wd.api.model.User;
import com.wd.api.repository.PermissionRepository;
import com.wd.api.repository.RoleRepository;
import com.wd.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private static final String ADMIN_ROLE_CODE = "ADMIN";

    /**
     * Get all permission names for a user
     * ADMIN users get ALL available permissions automatically
     */
    public List<String> getUserPermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return getUserPermissions(user);
    }

    /**
     * Get all permission names for a user
     * ADMIN users get ALL available permissions automatically
     */
    public List<String> getUserPermissions(User user) {
        if (user.getRole() == null) {
            return Collections.emptyList();
        }

        // ADMIN bypass: User is admin, so they have implicit access.
        // We don't need to return a list of permissions because the frontend
        // handles ADMIN role specifically (isAdmin=true bypassing checks).
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
    public boolean hasPermission(Long userId, String permission) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return hasPermission(user, permission);
    }

    /**
     * Check if user has a specific permission
     * ADMIN always returns true
     */
    public boolean hasPermission(User user, String permission) {
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
    public boolean hasAnyPermission(User user, List<String> permissions) {
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
    public boolean hasAllPermissions(User user, List<String> permissions) {
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
    public boolean isAdmin(User user) {
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
    public List<String> getRolePermissions(Long roleId) {
        Role role = roleRepository.findById(roleId)
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
    public boolean canView(User user, String module) {
        return hasPermission(user, module + "_VIEW");
    }

    /**
     * Helper method: Check if user can create in a module
     */
    public boolean canCreate(User user, String module) {
        return hasPermission(user, module + "_CREATE");
    }

    /**
     * Helper method: Check if user can edit in a module
     */
    public boolean canEdit(User user, String module) {
        return hasPermission(user, module + "_EDIT");
    }

    /**
     * Helper method: Check if user can delete in a module
     */
    public boolean canDelete(User user, String module) {
        return hasPermission(user, module + "_DELETE");
    }
}
