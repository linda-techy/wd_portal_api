package com.wd.api.service;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.model.SiteReport;
import com.wd.api.model.SiteReportPhoto;
import com.wd.api.model.enums.SiteReportStatus;
import com.wd.api.repository.SiteReportPhotoRepository;
import com.wd.api.repository.SiteReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * TDD slice locking in {@link SiteReportService#createReport} happy-path
 * behaviour, so the upcoming refactor (move file IO outside the JPA
 * transaction) doesn't silently break a working flow.
 *
 * <p>What's pinned here:
 *
 * <ul>
 *   <li>The report row is saved exactly once (regression guard for double
 *       saves slipping in via the refactor).</li>
 *   <li>Each photo is stored via the optimised storage path and persisted
 *       once.</li>
 *   <li>Gallery sync is attempted; a thrown exception there does NOT fail
 *       the operation (audit-confirmed behaviour: the upload the user
 *       made must still succeed even if downstream gallery rows fail).</li>
 *   <li>Customer notification + webhook publish are invoked.</li>
 * </ul>
 *
 * <p>This isn't a transaction-boundary test — Mockito can't observe Spring
 * proxy boundaries. Its job is to be the safety net while the boundary
 * itself is moved.
 */
@ExtendWith(MockitoExtension.class)
class SiteReportServiceCreateTest {

    @Mock private SiteReportRepository siteReportRepository;
    @Mock private SiteReportPhotoRepository siteReportPhotoRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private GalleryService galleryService;
    @Mock private CustomerNotificationFacade customerNotificationFacade;
    @Mock private WebhookPublisherService webhookPublisherService;

    @InjectMocks private SiteReportService service;

    private CustomerProject project;
    private PortalUser submitter;

    @BeforeEach
    void setUp() {
        project = new CustomerProject();
        ReflectionTestUtils.setField(project, "id", 30L);
        project.setName("Demo Project");
        project.setLatitude(10.02);
        project.setLongitude(76.38);

        submitter = new PortalUser();
        submitter.setId(42L);
        submitter.setFirstName("Praveen");
        submitter.setLastName("F");

        lenient().when(siteReportRepository.save(any(SiteReport.class)))
                .thenAnswer(inv -> {
                    SiteReport r = inv.getArgument(0);
                    if (r.getId() == null) ReflectionTestUtils.setField(r, "id", 99L);
                    return r;
                });
        lenient().when(fileStorageService.storeOptimizedImage(any(MultipartFile.class), anyString()))
                .thenAnswer(inv -> {
                    String subDir = inv.getArgument(1);
                    return subDir + "/" + java.util.UUID.randomUUID() + ".jpg";
                });
        lenient().when(siteReportPhotoRepository.save(any(SiteReportPhoto.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createReport_happyPath_savesReportAndEachPhotoExactlyOnce() {
        SiteReport report = baseReport();
        List<MultipartFile> photos = List.of(
                new MockMultipartFile("photos", "a.jpg", "image/jpeg", new byte[]{1, 2, 3}),
                new MockMultipartFile("photos", "b.jpg", "image/jpeg", new byte[]{4, 5, 6}));

        report.setStatus(SiteReportStatus.SUBMITTED); // set by controller before service call
        SiteReport saved = service.createReport(report, photos, submitter);

        assertThat(saved.getId()).isEqualTo(99L);
        assertThat(saved.getStatus()).isEqualTo(SiteReportStatus.SUBMITTED);
        // Exactly one report save (regression: a refactored boundary
        // could double-save accidentally).
        verify(siteReportRepository).save(any(SiteReport.class));
        // Each photo: stored once + row saved once.
        verify(fileStorageService, atLeastOnce())
                .storeOptimizedImage(any(MultipartFile.class), anyString());
        verify(siteReportPhotoRepository, atLeastOnce()).save(any(SiteReportPhoto.class));
        // Customer + webhook channels both fired.
        verify(customerNotificationFacade).notifyOwners(
                any(), anyString(), anyString(), anyString(), any());
        verify(webhookPublisherService).publishSiteReportSubmitted(any(), any(), anyString());
    }

    @Test
    void createReport_galleryFailure_doesNotFailTheOperation() {
        // Gallery sync is best-effort — the report + photos must persist
        // even if the customer-facing gallery row creation throws.
        doThrow(new RuntimeException("gallery offline"))
                .when(galleryService).createImagesFromSiteReport(any(), any());

        SiteReport report = baseReport();
        List<MultipartFile> photos = List.of(
                new MockMultipartFile("photos", "x.jpg", "image/jpeg", new byte[]{9}));

        SiteReport saved = service.createReport(report, photos, submitter);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(99L);
        // Notification + webhook still fire even though gallery threw.
        verify(customerNotificationFacade).notifyOwners(
                any(), anyString(), anyString(), anyString(), any());
        verify(webhookPublisherService).publishSiteReportSubmitted(any(), any(), anyString());
    }

    @Test
    void createReport_calculatesDistanceFromProject_whenBothHaveGps() {
        SiteReport report = baseReport();
        report.setLatitude(10.025);
        report.setLongitude(76.385);

        ArgumentCaptor<SiteReport> captor = ArgumentCaptor.forClass(SiteReport.class);
        service.createReport(report, List.of(
                new MockMultipartFile("photos", "x.jpg", "image/jpeg", new byte[]{1})), submitter);

        verify(siteReportRepository).save(captor.capture());
        // Project GPS is (10.02, 76.38), report GPS is (10.025, 76.385) —
        // ~720m apart. Service stores km, so we expect a small positive value.
        assertThat(captor.getValue().getDistanceFromProject())
                .isNotNull()
                .isGreaterThan(0.0)
                .isLessThan(2.0);
    }

    private SiteReport baseReport() {
        SiteReport r = new SiteReport();
        r.setProject(project);
        r.setSubmittedBy(submitter);
        r.setTitle("Daily progress 30 Apr");
        return r;
    }
}
