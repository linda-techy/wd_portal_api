package com.wd.api.service;

import com.wd.api.dto.SiteReportDto;
import com.wd.api.dto.SiteReportSearchFilter;
import com.wd.api.exception.BusinessException;
import com.wd.api.exception.ResourceNotFoundException;
import com.wd.api.model.PortalUser;
import com.wd.api.model.SiteReport;
import com.wd.api.model.SiteReportPhoto;
import com.wd.api.model.CustomerProject;
import com.wd.api.repository.SiteReportRepository;
import com.wd.api.repository.SiteReportPhotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SiteReportService {

    private static final Logger logger = LoggerFactory.getLogger(SiteReportService.class);

    private final SiteReportRepository siteReportRepository;
    private final SiteReportPhotoRepository siteReportPhotoRepository;
    private final FileStorageService fileStorageService;
    private final GalleryService galleryService;
    private final CustomerNotificationFacade customerNotificationFacade;
    private final WebhookPublisherService webhookPublisherService;

    public SiteReportService(SiteReportRepository siteReportRepository,
            SiteReportPhotoRepository siteReportPhotoRepository,
            FileStorageService fileStorageService,
            GalleryService galleryService,
            CustomerNotificationFacade customerNotificationFacade,
            WebhookPublisherService webhookPublisherService) {
        this.siteReportRepository = siteReportRepository;
        this.siteReportPhotoRepository = siteReportPhotoRepository;
        this.fileStorageService = fileStorageService;
        this.galleryService = galleryService;
        this.customerNotificationFacade = customerNotificationFacade;
        this.webhookPublisherService = webhookPublisherService;
    }

    /**
     * Search site reports with filters and pagination, returning the
     * customer-facing DTO (flat projectName, submittedByName, etc.). The
     * raw entity serialises with nested {@code project: { name }} +
     * {@code submittedBy: { firstName, lastName }} which (a) leaks
     * lazy-init proxies into JSON and (b) triggers N+1 selects per page.
     * The DTO is hydrated inside the read-only transaction so the LAZY
     * collections resolve cleanly before the session closes.
     */
    @Transactional(readOnly = true)
    public Page<SiteReportDto> searchSiteReports(SiteReportSearchFilter filter) {
        Specification<SiteReport> spec = buildSpecification(filter);
        return siteReportRepository.findAll(spec, filter.toPageable())
                .map(SiteReportDto::new);
    }

    private Specification<SiteReport> buildSpecification(SiteReportSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search across title, description, submitter name
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), searchPattern),
                        cb.like(cb.lower(root.get("description")), searchPattern),
                        cb.like(cb.lower(root.join("submittedBy").get("name")), searchPattern)));
            }

            // Filter by projectId
            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            }

            // Filter by reportType
            if (filter.getReportType() != null && !filter.getReportType().isEmpty()) {
                predicates.add(cb.equal(root.get("reportType"), filter.getReportType()));
            }

            // Filter by submitter user id. The DTO carries TWO aliases of
            // the same field (reportedBy + reportedById) for legacy reasons —
            // the Flutter provider sends `reportedBy`, but the original
            // service only honoured `reportedById`. Accept whichever the
            // caller populated so neither client silently no-ops.
            Long reporterId = filter.getReportedById() != null
                    ? filter.getReportedById()
                    : filter.getReportedBy();
            if (reporterId != null) {
                predicates.add(cb.equal(root.get("submittedBy").get("id"), reporterId));
            }

            // Filter by title
            if (filter.getTitle() != null && !filter.getTitle().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + filter.getTitle().toLowerCase() + "%"));
            }

            // Filter by status
            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            // Date range filter
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("reportDate"), filter.getStartDate().atStartOfDay()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("reportDate"), filter.getEndDate().atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public List<SiteReport> getReportsByProject(Long projectId) {
        return siteReportRepository.findByProjectIdOrderByReportDateDesc(projectId);
    }

    @Transactional(readOnly = true)
    public List<SiteReport> getReportsByUser(Long userId) {
        return siteReportRepository.findBySubmittedByIdOrderByReportDateDesc(userId);
    }

    @Transactional(readOnly = true)
    public SiteReport getReportById(Long id) {
        return siteReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SiteReport", id));
    }

    /**
     * Create a site report end-to-end: save the parent row, store + persist
     * each photo, sync to gallery, notify customers, publish a webhook.
     *
     * <p>This method is intentionally <b>not</b> {@code @Transactional}.
     * Site-report creation involves slow file IO (per-photo JPEG re-encoding
     * via {@link FileStorageService#storeOptimizedImage}) which previously
     * took ~20 seconds inside a single JPA transaction — long enough that
     * the hosted Postgres dropped the idle connection mid-transaction and
     * the final webhook insert blew up with "Connection is closed". Each
     * {@code repo.save(...)} call below opens its own auto-commit
     * transaction, which is exactly what we want — no DB connection is
     * held across the file IO loop.
     *
     * <p>Trade-off: not atomic across photos. If the report row saves but
     * a later photo write fails, you get an orphan report. The previous
     * design was already non-atomic (file storage is filesystem, not DB)
     * so this only makes the behaviour explicit. Reconciliation is via
     * the gallery-sync error key {@code GALLERY_SYNC_FAILED} +
     * webhook-retry mechanism (separate concern).
     */
    public SiteReport createReport(SiteReport report, List<MultipartFile> photos, PortalUser submittedBy) {
        // Calculate distance from project if GPS coordinates provided
        if (report.getLatitude() != null && report.getLongitude() != null && report.getProject() != null) {
            CustomerProject project = report.getProject();
            if (project.getLatitude() != null && project.getLongitude() != null) {
                Double distance = calculateDistance(
                    report.getLatitude(), report.getLongitude(),
                    project.getLatitude(), project.getLongitude()
                );
                report.setDistanceFromProject(distance);
            } else {
                // Project has no GPS lock yet → distanceFromProject stays
                // null. Surface a WARN so ops can prompt admins to set
                // the GPS lock on the project (V82 introduced the lock
                // UI). Silent skip used to be invisible.
                logger.warn(
                        "SITE_REPORT_NO_PROJECT_GPS projectId={} reportTitle={} — "
                                + "distanceFromProject will be null until project GPS is locked.",
                        project.getId(), report.getTitle());
            }
        }

        SiteReport savedReport = siteReportRepository.save(report);

        if (photos != null && !photos.isEmpty()) {
            int displayOrder = 0;
            for (MultipartFile photo : photos) {
                String subDir = "site-reports/" + savedReport.getId();
                // V84: re-encode the upload (typically a 5–10 MB phone
                // shot) as a quality-82 JPEG to slash storage by 50–70%.
                // Falls back to raw store for non-image uploads.
                String storedPath = fileStorageService.storeOptimizedImage(photo, subDir);

                SiteReportPhoto reportPhoto = new SiteReportPhoto();
                reportPhoto.setSiteReport(savedReport);
                reportPhoto.setStoragePath(storedPath);
                reportPhoto.setPhotoUrl("/api/storage/" + storedPath);
                reportPhoto.setDisplayOrder(displayOrder++);

                siteReportPhotoRepository.save(reportPhoto);
                savedReport.addPhoto(reportPhoto);
            }

            // Auto-sync site report photos to gallery. Failure here used
            // to be silently swallowed (the original audit flagged it as
            // CRITICAL): the report saves, photos save, but the gallery
            // never sees them and customers think nothing happened. Now
            // we (a) raise the log level + structured key so the failure
            // is greppable in production, (b) emit a webhook so an
            // out-of-band reconciler can retry. The whole site-report
            // operation still succeeds — we cannot let a downstream
            // gallery problem fail the upload the user already made.
            try {
                galleryService.createImagesFromSiteReport(savedReport.getId(), submittedBy);
                logger.info("Auto-synced {} photos from site report {} to gallery",
                        savedReport.getPhotos().size(), savedReport.getId());
            } catch (Exception e) {
                logger.error(
                        "GALLERY_SYNC_FAILED reportId={} photoCount={} error={} — "
                                + "report saved, photos saved, but gallery rows missing. "
                                + "Run reconciliation tool / re-trigger via admin endpoint.",
                        savedReport.getId(),
                        savedReport.getPhotos().size(),
                        e.getMessage(), e);
                // Don't fail the entire operation if gallery sync fails
            }
        }

        // Notify project customers (CUSTOMER + CUSTOMER_ADMIN) about the new site report
        if (savedReport.getProject() != null) {
            String reportTitle = savedReport.getTitle() != null ? savedReport.getTitle() : "Site Report";
            customerNotificationFacade.notifyOwners(
                    savedReport.getProject().getId(),
                    "New Site Report: " + reportTitle,
                    submittedBy.getFirstName() + " " + submittedBy.getLastName() + " submitted a new site report.",
                    "SITE_REPORT",
                    savedReport.getId()
            );
            // Webhook: notify Customer API so it can persist the notification in its own store
            webhookPublisherService.publishSiteReportSubmitted(
                    savedReport.getProject().getId(),
                    savedReport.getId(),
                    reportTitle);
        }

        return savedReport;
    }

    @Transactional
    public void deleteReport(Long id) {
        SiteReport report = getReportById(id);

        // Delete physical files
        for (SiteReportPhoto photo : report.getPhotos()) {
            fileStorageService.deleteFile(photo.getStoragePath());
        }

        siteReportRepository.delete(report);
    }

    @Transactional
    public SiteReport updateReport(SiteReport report) {
        return siteReportRepository.save(report);
    }

    /**
     * Same transaction-boundary rationale as {@link #createReport}:
     * file IO outside any DB transaction so a slow JPEG re-encode loop
     * cannot hold the connection open and trigger Postgres idle-cancel.
     */
    public SiteReport addPhotosToReport(Long reportId, List<MultipartFile> photos,
            List<Map<String, Object>> metadata, PortalUser currentUser) {

        SiteReport report = getReportById(reportId);

        // Verify ownership or admin rights
        if (!report.getSubmittedBy().getId().equals(currentUser.getId()) &&
            (currentUser.getRole() == null || !"ADMIN".equals(currentUser.getRole().getCode()))) {
            throw new BusinessException("You don't have permission to add photos to this report",
                HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        // V84.5 audit fixes: enforce input bounds before any DB write so a
        // single bad row doesn't leave the table in a partial state.
        SiteReportInputValidator.validatePhotoCount(photos);
        SiteReportInputValidator.validateMetadataAlignsWithPhotos(metadata, photos);

        // V84: avoid the O(N) stream over the in-memory photos collection
        // (which forced loading every photo on the LAZY association). The
        // repo query returns MAX(display_order) directly — one round-trip,
        // O(1) memory.
        Integer dbMax = siteReportPhotoRepository.findMaxDisplayOrderByReportId(reportId);
        int maxOrder = dbMax != null ? dbMax : -1;

        // Add new photos
        for (int i = 0; i < photos.size(); i++) {
            MultipartFile photo = photos.get(i);
            String subDir = "site-reports/" + reportId;
            // V84: same JPEG-optimisation as createReport.
            String storedPath = fileStorageService.storeOptimizedImage(photo, subDir);

            SiteReportPhoto reportPhoto = new SiteReportPhoto();
            reportPhoto.setSiteReport(report);
            reportPhoto.setStoragePath(storedPath);
            reportPhoto.setPhotoUrl("/api/storage/" + storedPath);
            reportPhoto.setDisplayOrder(++maxOrder);

            // Apply metadata if provided — bounds-checked
            if (metadata != null && i < metadata.size()) {
                Map<String, Object> meta = metadata.get(i);
                if (meta.containsKey("caption")) {
                    String caption = (String) meta.get("caption");
                    SiteReportInputValidator.validatePhotoCaptionLen(caption);
                    reportPhoto.setCaption(caption);
                }
                if (meta.containsKey("latitude") && meta.get("latitude") != null) {
                    Double lat = Double.valueOf(meta.get("latitude").toString());
                    SiteReportInputValidator.validateLatitude(lat, "photo");
                    reportPhoto.setLatitude(lat);
                }
                if (meta.containsKey("longitude") && meta.get("longitude") != null) {
                    Double lng = Double.valueOf(meta.get("longitude").toString());
                    SiteReportInputValidator.validateLongitude(lng, "photo");
                    reportPhoto.setLongitude(lng);
                }
            }

            siteReportPhotoRepository.save(reportPhoto);
            report.addPhoto(reportPhoto);
        }

        // Sync to gallery
        try {
            galleryService.createImagesFromSiteReport(reportId, currentUser);
            logger.info("Synced {} new photos from site report {} to gallery", 
                    photos.size(), reportId);
        } catch (Exception e) {
            logger.error("Failed to sync new photos to gallery: {}", e.getMessage(), e);
        }

        return report;
    }

    @Transactional
    public void deletePhoto(Long reportId, Long photoId, PortalUser currentUser) {
        SiteReport report = getReportById(reportId);

        // Verify ownership or admin rights
        if (!report.getSubmittedBy().getId().equals(currentUser.getId()) && 
            (currentUser.getRole() == null || !"ADMIN".equals(currentUser.getRole().getCode()))) {
            throw new BusinessException("You don't have permission to delete photos from this report", 
                HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        SiteReportPhoto photo = siteReportPhotoRepository.findById(photoId)
            .orElseThrow(() -> new ResourceNotFoundException("SiteReportPhoto", photoId));

        // Verify photo belongs to this report
        if (!photo.getSiteReport().getId().equals(reportId)) {
            throw new BusinessException("Photo does not belong to this report", 
                HttpStatus.BAD_REQUEST, "PHOTO_MISMATCH");
        }

        // Delete physical file
        fileStorageService.deleteFile(photo.getStoragePath());

        // Remove from report and delete
        report.removePhoto(photo);
        siteReportPhotoRepository.delete(photo);
    }

    @Transactional
    public void reorderPhotos(Long reportId, List<Long> photoIds, PortalUser currentUser) {
        SiteReport report = getReportById(reportId);

        // Verify ownership or admin rights
        if (!report.getSubmittedBy().getId().equals(currentUser.getId()) && 
            (currentUser.getRole() == null || !"ADMIN".equals(currentUser.getRole().getCode()))) {
            throw new BusinessException("You don't have permission to reorder photos in this report", 
                HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        // Update display order for each photo
        for (int i = 0; i < photoIds.size(); i++) {
            Long photoId = photoIds.get(i);
            SiteReportPhoto photo = siteReportPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("SiteReportPhoto", photoId));

            // Verify photo belongs to this report
            if (!photo.getSiteReport().getId().equals(reportId)) {
                throw new BusinessException("Photo " + photoId + " does not belong to this report", 
                    HttpStatus.BAD_REQUEST, "PHOTO_MISMATCH");
            }

            photo.setDisplayOrder(i);
            siteReportPhotoRepository.save(photo);
        }
    }

    /**
     * Calculate distance between two GPS coordinates using Haversine formula
     * @return distance in kilometers
     */
    private Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        final int EARTH_RADIUS_KM = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
