package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.GalleryImageDto;
import com.wd.api.dto.GallerySearchFilter;
import com.wd.api.model.PortalUser;
import com.wd.api.service.AuthService;
import com.wd.api.service.GalleryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gallery")
public class GalleryController {

    private static final Logger logger = LoggerFactory.getLogger(GalleryController.class);

    private final GalleryService galleryService;
    private final AuthService authService;

    public GalleryController(GalleryService galleryService, AuthService authService) {
        this.galleryService = galleryService;
        this.authService = authService;
    }

    @GetMapping("/search")
    public ResponseEntity<Page<GalleryImageDto>> searchGalleryImages(@ModelAttribute GallerySearchFilter filter) {
        try {
            Page<GalleryImageDto> images = galleryService.searchGalleryImages(filter);
            return ResponseEntity.ok(images);
        } catch (Exception e) {
            logger.error("Error searching gallery images", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<GalleryImageDto>>> getImagesByProject(@PathVariable Long projectId) {
        try {
            List<GalleryImageDto> images = galleryService.getImagesByProject(projectId);
            return ResponseEntity.ok(ApiResponse.success("Gallery images retrieved successfully", images));
        } catch (Exception e) {
            logger.error("Error fetching gallery images for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/project/{projectId}/grouped")
    public ResponseEntity<ApiResponse<Map<LocalDate, List<GalleryImageDto>>>> getImagesGroupedByDate(
            @PathVariable Long projectId) {
        try {
            Map<LocalDate, List<GalleryImageDto>> groupedImages = galleryService.getImagesGroupedByDate(projectId);
            return ResponseEntity.ok(ApiResponse.success("Gallery images grouped by date", groupedImages));
        } catch (Exception e) {
            logger.error("Error fetching grouped gallery images for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GalleryImageDto>> getImageById(@PathVariable Long id) {
        try {
            GalleryImageDto image = galleryService.getImageById(id);
            return ResponseEntity.ok(ApiResponse.success("Gallery image retrieved successfully", image));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching gallery image {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @PostMapping(value = "/project/{projectId}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<GalleryImageDto>> uploadImage(
            @PathVariable Long projectId,
            @RequestPart("image") MultipartFile image,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String locationTag,
            @RequestParam(required = false) String[] tags,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate takenDate) {
        try {
            if (image.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Image file is required"));
            }

            PortalUser currentUser = authService.getCurrentUser();
            GalleryImageDto savedImage = galleryService.uploadImage(
                    projectId, image, caption, locationTag, tags, takenDate, currentUser);

            return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", savedImage));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error uploading gallery image for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @PostMapping(value = "/project/{projectId}/batch", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<List<GalleryImageDto>>> uploadMultipleImages(
            @PathVariable Long projectId,
            @RequestPart("images") List<MultipartFile> images,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String locationTag,
            @RequestParam(required = false) String[] tags,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate takenDate) {
        try {
            if (images == null || images.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("At least one image is required"));
            }

            PortalUser currentUser = authService.getCurrentUser();
            List<GalleryImageDto> savedImages = images.stream()
                    .filter(img -> !img.isEmpty())
                    .map(img -> galleryService.uploadImage(
                            projectId, img, caption, locationTag, tags, takenDate, currentUser))
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(
                    savedImages.size() + " images uploaded successfully", savedImages));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error uploading batch gallery images for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GalleryImageDto>> updateImage(
            @PathVariable Long id,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String locationTag,
            @RequestParam(required = false) String[] tags) {
        try {
            GalleryImageDto updatedImage = galleryService.updateImage(id, caption, locationTag, tags);
            return ResponseEntity.ok(ApiResponse.success("Image updated successfully", updatedImage));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating gallery image {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable Long id) {
        try {
            galleryService.deleteImage(id);
            return ResponseEntity.ok(ApiResponse.success("Image deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting gallery image {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/project/{projectId}/count")
    public ResponseEntity<ApiResponse<Long>> getImageCount(@PathVariable Long projectId) {
        try {
            long count = galleryService.getImageCountByProject(projectId);
            return ResponseEntity.ok(ApiResponse.success("Image count retrieved", count));
        } catch (Exception e) {
            logger.error("Error getting image count for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }
}
