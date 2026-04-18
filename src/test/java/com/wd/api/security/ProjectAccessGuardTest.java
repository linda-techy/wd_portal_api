package com.wd.api.security;

import com.wd.api.repository.ProjectMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProjectAccessGuard.
 * Sets up a SecurityContext directly — no Spring MVC or JWT stack needed.
 */
@ExtendWith(MockitoExtension.class)
class ProjectAccessGuardTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectAccessGuard projectAccessGuard;

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void restoreContext() {
        SecurityContextHolder.clearContext();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setAuthentication(String... roles) {
        List<GrantedAuthority> authorities = List.of(roles).stream()
                .map(SimpleGrantedAuthority::new)
                .map(a -> (GrantedAuthority) a)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@test.com", null, authorities));
    }

    // ── verifyPortalAccess ────────────────────────────────────────────────────

    @Test
    void verifyPortalAccess_adminRole_bypassesMembershipCheck() {
        setAuthentication("ROLE_ADMIN");

        // Should not throw — admin bypasses membership
        assertThatCode(() -> projectAccessGuard.verifyPortalAccess(1L, 10L))
                .doesNotThrowAnyException();

        verify(projectMemberRepository, never()).existsByProject_IdAndPortalUser_Id(anyLong(), anyLong());
    }

    @Test
    void verifyPortalAccess_directorRole_bypassesMembershipCheck() {
        setAuthentication("ROLE_DIRECTOR");

        assertThatCode(() -> projectAccessGuard.verifyPortalAccess(1L, 10L))
                .doesNotThrowAnyException();

        verify(projectMemberRepository, never()).existsByProject_IdAndPortalUser_Id(anyLong(), anyLong());
    }

    @Test
    void verifyPortalAccess_regularUserWithMembership_passes() {
        setAuthentication("ROLE_ENGINEER");
        when(projectMemberRepository.existsByProject_IdAndPortalUser_Id(10L, 1L)).thenReturn(true);

        assertThatCode(() -> projectAccessGuard.verifyPortalAccess(1L, 10L))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyPortalAccess_regularUserWithoutMembership_throwsAccessDeniedException() {
        setAuthentication("ROLE_ENGINEER");
        when(projectMemberRepository.existsByProject_IdAndPortalUser_Id(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> projectAccessGuard.verifyPortalAccess(1L, 10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("project 10");
    }

    @Test
    void verifyPortalAccess_noAuthentication_throwsAccessDeniedException() {
        // SecurityContext is empty (no authentication set)
        assertThatThrownBy(() -> projectAccessGuard.verifyPortalAccess(1L, 10L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
