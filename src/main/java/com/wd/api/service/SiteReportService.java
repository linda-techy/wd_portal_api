package com.wd.api.service;

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

    public SiteReportService(SiteReportRepository siteReportRepository,
            SiteReportPhotoRepository siteReportPhotoRepository,
            FileStorageService fileStorageService,
            GalleryService galleryService) {
        this.siteReportRepository = siteReportRepository;
        this.siteReportPhotoRepository = siteReportPhotoRepository;
        this.fileStorageService = fileStorageService;
        this.galleryService = galleryService;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<SiteReport> searchSiteReports(SiteReportSearchFilter filter) {
        Specification<SiteReport> spec = buildSpecification(filter);
        return siteReportRepository.findAll(spec, filter.toPageable());
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

            // Filter by reportedById (submittedBy)
            if (filter.getReportedById() != null) {
                predicates.add(cb.equal(root.get("submittedBy").get("id"), filter.getReportedById()));
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
    @SuppressWarnings("null")
    public SiteReport getReportById(Long id) {
        return siteReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SiteReport", id));
    }

    @Transactional
    @SuppressWarnings("null")
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
            }
        }

        SiteReport savedReport = siteReportRepository.save(report);

        if (photos != null && !photos.isEmpty()) {
            int displayOrder = 0;
            for (MultipartFile photo : photos) {
                String subDir = "site-reports/" + savedReport.getId();
                String storedPath = fileStorageService.storeFile(photo, subDir);

                SiteReportPhoto reportPhoto = new SiteReportPhoto();
                reportPhoto.setSiteReport(savedReport);
                reportPhoto.setStoragePath(storedPath);
                reportPhoto.setPhotoUrl("/api/storage/" + storedPath);
                reportPhoto.setDisplayOrder(displayOrder++);

                siteReportPhotoRepository.save(reportPhoto);
                savedReport.addPhoto(reportPhoto);
            }

            // Auto-sync site report photos to gallery
            try {
                galleryService.createImagesFromSiteReport(savedReport.getId(), submittedBy);
                logger.info("Auto-synced {} photos from site report {} to gallery", 
                        savedReport.getPhotos().size(), savedReport.getId());
            } catch (Exception e) {
                logger.error("Failed to sync site report photos to gallery: {}", e.getMessage(), e);
                // Don't fail the entire operation if gallery sync fails
            }
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
    @SuppressWarnings("null")
    public SiteReport updateReport(SiteReport report) {
        return siteReportRepository.save(report);
    }

    @Transactional
    @SuppressWarnings("null")
    public SiteReport addPhotosToReport(Long reportId, List<MultipartFile> photos, 
            List<Map<String, Object>> metadata, PortalUser currentUser) {
        
        SiteReport report = getReportById(reportId);

        // Verify ownership or admin rights
        if (!report.getSubmittedBy().getId().equals(currentUser.getId()) && 
            (currentUser.getRole() == null || !"ADMIN".equals(currentUser.getRole().getCode()))) {
            throw new BusinessException("You don't have permission to add photos to this report", 
                HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        // Get current max display order
        int maxOrder = report.getPhotos().stream()
            .mapToInt(SiteReportPhoto::getDisplayOrder)
            .max()
            .orElse(-1);

        // Add new photos
        for (int i = 0; i < photos.size(); i++) {
            MultipartFile photo = photos.get(i);
            String subDir = "site-reports/" + reportId;
            String storedPath = fileStorageService.storeFile(photo, subDir);

            SiteReportPhoto reportPhoto = new SiteReportPhoto();
            reportPhoto.setSiteReport(report);
            reportPhoto.setStoragePath(storedPath);
            reportPhoto.setPhotoUrl("/api/storage/" + storedPath);
            reportPhoto.setDisplayOrder(++maxOrder);

            // Apply metadata if provided
            if (metadata != null && i < metadata.size()) {
                Map<String, Object> meta = metadata.get(i);
                if (meta.containsKey("caption")) {
                    reportPhoto.setCaption((String) meta.get("caption"));
                }
                if (meta.containsKey("latitude") && meta.get("latitude") != null) {
                    reportPhoto.setLatitude(Double.valueOf(meta.get("latitude").toString()));
                }
                if (meta.containsKey("longitude") && meta.get("longitude") != null) {
                    reportPhoto.setLongitude(Double.valueOf(meta.get("longitude").toString()));
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
    @SuppressWarnings("null")
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
    @SuppressWarnings("null")
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
