package com.wd.api.service;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalRole;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.CustomerProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD slice for project GPS lock-and-override behaviour.
 *
 * <p>Goal: a project's site GPS coordinates are settable any-time-once by
 * any user with PROJECT_EDIT, but once stamped (gps_locked_at != null),
 * only ADMIN can override. The site-visit check-in flow already enforces
 * a 2km proximity guard against (latitude, longitude) — this slice adds
 * the missing "set + lock + override" capability so projects actually
 * have coordinates the check-in path can validate against.
 */
@ExtendWith(MockitoExtension.class)
class CustomerProjectServiceGpsLockTest {

    @Mock private CustomerProjectRepository projectRepository;

    private CustomerProjectService service;

    @BeforeEach
    void setUp() {
        service = new CustomerProjectService();
        ReflectionTestUtils.setField(service, "customerProjectRepository", projectRepository);

        lenient().when(projectRepository.save(any(CustomerProject.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void lockProjectGps_firstTime_setsCoordinatesAndStampsLock() {
        CustomerProject project = projectFresh(7L);
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));

        PortalUser anySiteEngineer = userWithRole(42L, "SITE_ENGINEER");

        CustomerProject saved = service.lockProjectGps(7L, 10.02, 76.38, anySiteEngineer);

        assertThat(saved.getLatitude()).isEqualTo(10.02);
        assertThat(saved.getLongitude()).isEqualTo(76.38);
        assertThat(saved.getGpsLockedAt()).isNotNull();
        assertThat(saved.getGpsLockedByUserId()).isEqualTo(42L);
        verify(projectRepository).save(project);
    }

    @Test
    void lockProjectGps_alreadyLocked_nonAdminUser_throws() {
        CustomerProject project = projectAlreadyLocked(7L);
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));

        PortalUser nonAdmin = userWithRole(42L, "SITE_ENGINEER");

        assertThatThrownBy(() -> service.lockProjectGps(7L, 10.05, 76.40, nonAdmin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("locked")
                .hasMessageContaining("Administrator");
    }

    @Test
    void lockProjectGps_alreadyLocked_admin_overridesAndRestamps() {
        CustomerProject project = projectAlreadyLocked(7L);
        LocalDateTime originalLockTime = project.getGpsLockedAt();
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));

        PortalUser admin = userWithRole(99L, "ADMIN");

        CustomerProject saved = service.lockProjectGps(7L, 10.05, 76.40, admin);

        assertThat(saved.getLatitude()).isEqualTo(10.05);
        assertThat(saved.getLongitude()).isEqualTo(76.40);
        assertThat(saved.getGpsLockedByUserId()).isEqualTo(99L);
        assertThat(saved.getGpsLockedAt())
                .as("admin override re-stamps the lock timestamp")
                .isAfterOrEqualTo(originalLockTime);
    }

    @Test
    void lockProjectGps_rejectsNullIslandCoordinates() {
        // No findById stub on purpose — the (0,0) guard must fire before
        // the service ever touches the repository (fail fast, save a query).
        assertThatThrownBy(() -> service.lockProjectGps(
                7L, 0.0, 0.0, userWithRole(42L, "SITE_ENGINEER")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GPS");
    }

    private CustomerProject projectFresh(Long id) {
        CustomerProject p = new CustomerProject();
        p.setId(id);
        return p;
    }

    private CustomerProject projectAlreadyLocked(Long id) {
        CustomerProject p = projectFresh(id);
        p.setLatitude(10.0);
        p.setLongitude(76.0);
        p.setGpsLockedAt(LocalDateTime.now().minusDays(7));
        p.setGpsLockedByUserId(11L);
        return p;
    }

    private PortalUser userWithRole(Long id, String roleCode) {
        PortalUser u = new PortalUser();
        u.setId(id);
        PortalRole r = new PortalRole();
        r.setCode(roleCode);
        u.setRole(r);
        return u;
    }
}
