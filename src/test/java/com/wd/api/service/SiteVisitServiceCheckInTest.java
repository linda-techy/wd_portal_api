package com.wd.api.service;

import com.wd.api.dto.CheckInRequest;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.SiteVisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SiteVisitService#checkIn} — TDD slice.
 *
 * <p>Targets the well-known mobile "Null Island" bug: an Android device
 * whose GPS sensor hasn't yet acquired a fix returns lat/lng = 0.0 / 0.0
 * (the literal coordinates of the Gulf of Guinea, "null island"). The
 * current service accepts (0, 0) as valid because it only null-checks the
 * Doubles, then the proximity guard rejects with a misleading
 * "you are ~11,000 km away" message. Right answer: explicitly reject
 * (0, 0) with an actionable "GPS not ready, retry shortly" message.
 */
@ExtendWith(MockitoExtension.class)
class SiteVisitServiceCheckInTest {

    @Mock private SiteVisitRepository siteVisitRepository;
    @Mock private CustomerProjectRepository projectRepository;
    @Mock private PortalUserRepository portalUserRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    private SiteVisitService service;

    @BeforeEach
    void setUp() {
        service = new SiteVisitService(
                siteVisitRepository,
                projectRepository,
                portalUserRepository,
                jdbcTemplate);

        // Default stubs so checkIn reaches the GPS-validation block. Tests
        // that exercise upstream guards (active visit, missing project, etc.)
        // can override these via specific when(...) calls.
        CustomerProject project = new CustomerProject();
        project.setId(1L);
        project.setName("Demo Project");
        project.setLatitude(10.02);
        project.setLongitude(76.38);

        PortalUser user = new PortalUser();
        user.setId(99L);

        lenient().when(siteVisitRepository.findActiveVisitByUser(99L))
                .thenReturn(Optional.empty());
        lenient().when(projectRepository.findById(1L))
                .thenReturn(Optional.of(project));
        lenient().when(portalUserRepository.findById(99L))
                .thenReturn(Optional.of(user));
    }

    @Test
    void checkIn_rejectsNullIslandCoordinatesAsUninitialisedSensor() {
        CheckInRequest req = new CheckInRequest();
        req.setProjectId(1L);
        req.setLatitude(0.0);
        req.setLongitude(0.0);

        assertThatThrownBy(() -> service.checkIn(req, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GPS")
                .hasMessageContaining("not yet ready");
    }
}
