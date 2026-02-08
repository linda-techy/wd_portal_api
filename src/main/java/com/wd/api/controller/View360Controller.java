package com.wd.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.model.View360;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.service.View360Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/view360")
@RequiredArgsConstructor
public class View360Controller {

    private final View360Service view360Service;
    private final com.wd.api.repository.PortalUserRepository portalUserRepository;
    private final CustomerProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<View360>> getProjectTours(@PathVariable Long projectId) {
        return ResponseEntity.ok(view360Service.getProjectTours(projectId));
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<View360> uploadTour(
            @RequestPart("tour") String tourJson,
            @RequestPart("file") MultipartFile file) throws Exception {

        @SuppressWarnings("unchecked")
        Map<String, Object> tourData = objectMapper.readValue(tourJson, Map.class);

        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
        PortalUser currentUser = portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Object projectIdObj = tourData.get("projectId");
        if (projectIdObj == null) {
            throw new RuntimeException("projectId is required");
        }
        Long projectId = Long.valueOf(projectIdObj.toString());
        @SuppressWarnings("null")
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        View360 tour = new View360();
        tour.setProject(project);
        tour.setTitle((String) tourData.get("title"));
        tour.setDescription((String) tourData.get("description"));
        tour.setLocation((String) tourData.get("location"));
        tour.setUploadedBy(currentUser);

        if (tourData.containsKey("captureDate")) {
            tour.setCaptureDate(LocalDateTime.parse((String) tourData.get("captureDate")));
        } else {
            tour.setCaptureDate(LocalDateTime.now());
        }

        return ResponseEntity.ok(view360Service.createTour(tour, file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<View360> getTour(@PathVariable Long id) {
        return ResponseEntity.ok(view360Service.getTour(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTour(@PathVariable Long id) {
        view360Service.deleteTour(id);
        return ResponseEntity.noContent().build();
    }
}
