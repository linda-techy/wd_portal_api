package com.wd.api.service;

import com.wd.api.model.SiteReport;
import com.wd.api.model.SiteReportPhoto;
import com.wd.api.repository.SiteReportRepository;
import com.wd.api.repository.SiteReportPhotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
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
                .orElseThrow(() -> new RuntimeException("Site Report not found with id: " + id));
    }

    @Transactional
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
    public void deleteReport(Long id) {
        SiteReport report = getReportById(id);

        // Delete physical files
        for (SiteReportPhoto photo : report.getPhotos()) {
            fileStorageService.deleteFile(photo.getStoragePath());
        }

        siteReportRepository.delete(report);
    }
}
