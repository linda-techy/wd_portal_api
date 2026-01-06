package com.wd.api.service;

import com.wd.api.model.View360;
import com.wd.api.repository.View360Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class View360Service {

    private final View360Repository view360Repository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<View360> getProjectTours(Long projectId) {
        return view360Repository.findByProjectIdOrderByCaptureDateDesc(projectId);
    }

    @Transactional
    public View360 createTour(View360 tour, MultipartFile panoramaFile) {
        // Store the main panorama image
        String subDir = "projects/" + tour.getProject().getId() + "/360";
        String storedPath = fileStorageService.storeFile(panoramaFile, subDir);

        tour.setPanoramaUrl("/api/files/download/" + storedPath);
        // For now, thumbnail is the same as panorama or a placeholder
        // TODO: Generate actual thumbnail in production
        tour.setThumbnailUrl(tour.getPanoramaUrl());

        if (tour.getCaptureDate() == null) {
            tour.setCaptureDate(LocalDateTime.now());
        }

        return view360Repository.save(tour);
    }

    @Transactional(readOnly = true)
    public View360 getTour(Long id) {
        return view360Repository.findById(id)
                .orElseThrow(() -> new RuntimeException("360 View not found"));
    }

    @Transactional
    public void deleteTour(Long id) {
        View360 tour = getTour(id);
        // Delete physical file
        if (tour.getPanoramaUrl() != null && tour.getPanoramaUrl().contains("/api/files/download/")) {
            String filePath = tour.getPanoramaUrl().replace("/api/files/download/", "");
            fileStorageService.deleteFile(filePath);
        }
        view360Repository.delete(tour);
    }
}
