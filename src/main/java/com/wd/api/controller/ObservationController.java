package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.ObservationDto;
import com.wd.api.dto.ObservationSearchFilter;
import com.wd.api.model.PortalUser;
import com.wd.api.service.AuthService;
import com.wd.api.service.ObservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/observations")
public class ObservationController {

    private static final Logger logger = LoggerFactory.getLogger(ObservationController.class);

    private final ObservationService observationService;
    private final AuthService authService;

    public ObservationController(ObservationService observationService, AuthService authService) {
        this.observationService = observationService;
        this.authService = authService;
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ObservationDto>> searchObservations(@ModelAttribute ObservationSearchFilter filter) {
        try {
            Page<ObservationDto> observations = observationService.searchObservations(filter);
            return ResponseEntity.ok(observations);
        } catch (Exception e) {
            logger.error("Error searching observations", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<ObservationDto>>> getObservationsByProject(@PathVariable Long projectId) {
        try {
            List<ObservationDto> observations = observationService.getObservationsByProject(projectId);
            return ResponseEntity.ok(ApiResponse.success("Observations retrieved successfully", observations));
        } catch (Exception e) {
            logger.error("Error fetching observations for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/project/{projectId}/active")
    public ResponseEntity<ApiResponse<List<ObservationDto>>> getActiveObservations(@PathVariable Long projectId) {
        try {
            List<ObservationDto> observations = observationService.getActiveObservationsByProject(projectId);
            return ResponseEntity.ok(ApiResponse.success("Active observations retrieved", observations));
        } catch (Exception e) {
            logger.error("Error fetching active observations for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/project/{projectId}/resolved")
    public ResponseEntity<ApiResponse<List<ObservationDto>>> getResolvedObservations(@PathVariable Long projectId) {
        try {
            List<ObservationDto> observations = observationService.getResolvedObservationsByProject(projectId);
            return ResponseEntity.ok(ApiResponse.success("Resolved observations retrieved", observations));
        } catch (Exception e) {
            logger.error("Error fetching resolved observations for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/project/{projectId}/counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getObservationCounts(@PathVariable Long projectId) {
        try {
            Map<String, Long> counts = observationService.getObservationCountsByProject(projectId);
            return ResponseEntity.ok(ApiResponse.success("Observation counts retrieved", counts));
        } catch (Exception e) {
            logger.error("Error fetching observation counts for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ObservationDto>>> getMyObservations() {
        try {
            PortalUser currentUser = authService.getCurrentUser();
            List<ObservationDto> observations = observationService.getMyObservations(currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("My observations retrieved", observations));
        } catch (Exception e) {
            logger.error("Error fetching my observations", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ObservationDto>> getObservationById(@PathVariable Long id) {
        try {
            ObservationDto observation = observationService.getObservationById(id);
            return ResponseEntity.ok(ApiResponse.success("Observation retrieved successfully", observation));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching observation {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @PostMapping(value = "/project/{projectId}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ObservationDto>> createObservation(
            @PathVariable Long projectId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Long reportedByRoleId,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            PortalUser currentUser = authService.getCurrentUser();
            ObservationDto observation = observationService.createObservation(
                    projectId, title, description, location, priority, severity,
                    image, currentUser, reportedByRoleId);

            return ResponseEntity.ok(ApiResponse.success("Observation created successfully", observation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating observation for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ObservationDto>> updateObservation(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            ObservationDto observation = observationService.updateObservation(
                    id, title, description, location, priority, severity, status, image);
            return ResponseEntity.ok(ApiResponse.success("Observation updated successfully", observation));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating observation {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<ObservationDto>> resolveObservation(
            @PathVariable Long id,
            @RequestParam(required = false) String resolutionNotes) {
        try {
            PortalUser currentUser = authService.getCurrentUser();
            ObservationDto observation = observationService.resolveObservation(id, resolutionNotes, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Observation resolved successfully", observation));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error resolving observation {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ObservationDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            ObservationDto observation = observationService.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Status updated successfully", observation));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating observation status {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteObservation(@PathVariable Long id) {
        try {
            observationService.deleteObservation(id);
            return ResponseEntity.ok(ApiResponse.success("Observation deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting observation {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }
}
