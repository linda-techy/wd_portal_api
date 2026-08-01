package com.wd.api.service;

import com.wd.api.model.View360;
import com.wd.api.repository.View360Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class View360Service {

    private static final String STORAGE_URL_PREFIX = "/api/storage/";
    private static final String KEY_TITLE = "title";
    private static final String KEY_CAPTURE_DATE = "captureDate";

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

        tour.setPanoramaUrl(STORAGE_URL_PREFIX + storedPath);
        // For now, thumbnail is the same as panorama or a placeholder
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
    public View360 updateTour(Long id, Map<String, Object> updates) {
        View360 tour = getTour(id);
        if (updates.containsKey(KEY_TITLE) && updates.get(KEY_TITLE) != null) {
            tour.setTitle((String) updates.get(KEY_TITLE));
        }
        if (updates.containsKey("description")) {
            tour.setDescription((String) updates.get("description"));
        }
        if (updates.containsKey("location")) {
            tour.setLocation((String) updates.get("location"));
        }
        if (updates.containsKey(KEY_CAPTURE_DATE) && updates.get(KEY_CAPTURE_DATE) != null) {
            tour.setCaptureDate(LocalDateTime.parse((String) updates.get(KEY_CAPTURE_DATE)));
        }
        return view360Repository.save(tour);
    }

    @Transactional
    public void deleteTour(Long id) {
        View360 tour = getTour(id);
        // Delete physical file
        if (tour.getPanoramaUrl() != null && tour.getPanoramaUrl().contains(STORAGE_URL_PREFIX)) {
            String filePath = tour.getPanoramaUrl().replace(STORAGE_URL_PREFIX, "");
            fileStorageService.deleteFile(filePath);
        }
        view360Repository.delete(tour);
    }
}
