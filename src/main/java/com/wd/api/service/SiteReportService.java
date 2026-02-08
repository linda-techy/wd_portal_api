package com.wd.api.service;

import com.wd.api.dto.SiteReportSearchFilter;
import com.wd.api.model.SiteReport;
import com.wd.api.model.SiteReportPhoto;
import com.wd.api.repository.SiteReportRepository;
import com.wd.api.repository.SiteReportPhotoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SiteReportService {

    private final SiteReportRepository siteReportRepository;
    private final SiteReportPhotoRepository siteReportPhotoRepository;
    private final FileStorageService fileStorageService;

    public SiteReportService(SiteReportRepository siteReportRepository,
            SiteReportPhotoRepository siteReportPhotoRepository,
            FileStorageService fileStorageService) {
        this.siteReportRepository = siteReportRepository;
        this.siteReportPhotoRepository = siteReportPhotoRepository;
        this.fileStorageService = fileStorageService;
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
                .orElseThrow(() -> new RuntimeException("Site Report not found with id: " + id));
    }

    @Transactional
    @SuppressWarnings("null")
    public SiteReport createReport(SiteReport report, List<MultipartFile> photos) {
        SiteReport savedReport = siteReportRepository.save(report);

        if (photos != null && !photos.isEmpty()) {
            for (MultipartFile photo : photos) {
                String subDir = "site-reports/" + savedReport.getId();
                String storedPath = fileStorageService.storeFile(photo, subDir);

                SiteReportPhoto reportPhoto = new SiteReportPhoto();
                reportPhoto.setSiteReport(savedReport);
                reportPhoto.setStoragePath(storedPath);
                reportPhoto.setPhotoUrl("/api/files/download/" + storedPath); // Simplified URL

                siteReportPhotoRepository.save(reportPhoto);
                savedReport.addPhoto(reportPhoto);
            }
        }

        return savedReport;
    }

    @Transactional
    @SuppressWarnings("null")
    public void deleteReport(Long id) {
        SiteReport report = getReportById(id);

        // Delete physical files
        for (SiteReportPhoto photo : report.getPhotos()) {
            fileStorageService.deleteFile(photo.getStoragePath());
        }

        siteReportRepository.delete(report);
    }
}
