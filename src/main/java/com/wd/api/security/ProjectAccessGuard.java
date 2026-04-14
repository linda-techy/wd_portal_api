package com.wd.api.security;

import com.wd.api.repository.ProjectMemberRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Enforces project-level access control for BOQ operations.
 *
 * Portal rules:
 *   ADMIN and DIRECTOR roles bypass the membership check (global access).
 *   All other roles must appear in project_members for the requested project.
 *
 * Customer rules:
 *   Customer users must appear in project_members for the requested project.
 */
@Component
public class ProjectAccessGuard {

    private static final java.util.Set<String> GLOBAL_ACCESS_ROLES =
            java.util.Set.of("ROLE_ADMIN", "ROLE_DIRECTOR");

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectAccessGuard(ProjectMemberRepository projectMemberRepository) {
        this.projectMemberRepository = projectMemberRepository;
    }

    /**
     * Verifies that the current portal user may access the given project.
     * Reads authorities from the current SecurityContext — no extra DB query for role lookup.
     *
     * @throws AccessDeniedException (HTTP 403) if access is not permitted
     */
    public void verifyPortalAccess(Long portalUserId, Long projectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("You do not have access to project " + projectId);
        }
        if (hasGlobalAccess(auth)) {
            return;
        }
        if (!projectMemberRepository.existsByProject_IdAndPortalUser_Id(projectId, portalUserId)) {
            throw new AccessDeniedException("You do not have access to project " + projectId);
        }
    }

    /**
     * Verifies that the given customer user may access the given project.
     *
     * @throws AccessDeniedException (HTTP 403) if not a project member
     */
    public void verifyCustomerAccess(Long customerUserId, Long projectId) {
        if (!projectMemberRepository.existsByProject_IdAndCustomerUser_Id(projectId, customerUserId)) {
            throw new AccessDeniedException("You do not have access to project " + projectId);
        }
    }

    private boolean hasGlobalAccess(Authentication auth) {
        if (auth == null) return false;
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(GLOBAL_ACCESS_ROLES::contains);
    }
}
