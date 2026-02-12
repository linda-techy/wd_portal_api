package com.wd.api.service;

import com.wd.api.dto.GalleryImageDto;
import com.wd.api.dto.GallerySearchFilter;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.GalleryImage;
import com.wd.api.model.PortalUser;
import com.wd.api.model.SiteReport;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.GalleryImageRepository;
import com.wd.api.repository.SiteReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GalleryService {

    private static final Logger logger = LoggerFactory.getLogger(GalleryService.class);

    private final GalleryImageRepository galleryImageRepository;
    private final CustomerProjectRepository projectRepository;
    private final SiteReportRepository siteReportRepository;
    private final FileStorageService fileStorageService;

    public GalleryService(GalleryImageRepository galleryImageRepository,
            CustomerProjectRepository projectRepository,
            SiteReportRepository siteReportRepository,
            FileStorageService fileStorageService) {
        this.galleryImageRepository = galleryImageRepository;
        this.projectRepository = projectRepository;
        this.siteReportRepository = siteReportRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<GalleryImageDto> searchGalleryImages(GallerySearchFilter filter) {
        Specification<GalleryImage> spec = buildSpecification(filter);
        Page<GalleryImage> images = galleryImageRepository.findAll(spec, filter.toPageable());
        return images.map(GalleryImageDto::fromEntity);
    }

    private Specification<GalleryImage> buildSpecification(GallerySearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("caption")), searchPattern),
                        cb.like(cb.lower(root.get("locationTag")), searchPattern)));
            }

            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            }

            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("takenDate"), filter.getStartDate()));
            }

            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("takenDate"), filter.getEndDate()));
            }

            if (filter.getLocationTag() != null && !filter.getLocationTag().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("locationTag")),
                        "%" + filter.getLocationTag().toLowerCase() + "%"));
            }

            if (filter.getUploadedById() != null) {
                predicates.add(cb.equal(root.get("uploadedBy").get("id"), filter.getUploadedById()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public List<GalleryImageDto> getImagesByProject(Long projectId) {
        List<GalleryImage> images = galleryImageRepository.findByProjectIdOrderByTakenDateDesc(projectId);
        return images.stream()
                .map(GalleryImageDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, List<GalleryImageDto>> getImagesGroupedByDate(Long projectId) {
        List<LocalDate> dates = galleryImageRepository.findDistinctTakenDatesByProjectId(projectId);
        Map<LocalDate, List<GalleryImageDto>> groupedImages = new LinkedHashMap<>();

        for (LocalDate date : dates) {
            List<GalleryImage> images = galleryImageRepository.findByProjectIdAndTakenDate(projectId, date);
            groupedImages.put(date, images.stream()
                    .map(GalleryImageDto::fromEntity)
                    .collect(Collectors.toList()));
        }

        return groupedImages;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public GalleryImageDto getImageById(Long id) {
        GalleryImage image = galleryImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gallery image not found with id: " + id));
        return GalleryImageDto.fromEntity(image);
    }

    @Transactional
    @SuppressWarnings("null")
    public GalleryImageDto uploadImage(Long projectId, MultipartFile file, String caption,
            String locationTag, String[] tags, LocalDate takenDate,
            PortalUser uploadedBy) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        String subDir = "gallery/" + projectId;
        String storedPath = fileStorageService.storeFile(file, subDir);

        GalleryImage image = new GalleryImage();
        image.setProject(project);
        image.setImagePath(storedPath);
        image.setImageUrl("/api/storage/" + storedPath);
        image.setCaption(caption);
        image.setLocationTag(locationTag);
        image.setTags(tags);
        image.setTakenDate(takenDate != null ? takenDate : LocalDate.now());
        image.setUploadedBy(uploadedBy);
        image.setUploadedAt(LocalDateTime.now());

        GalleryImage savedImage = galleryImageRepository.save(image);
        logger.info("Gallery image uploaded for project {}: {}", projectId, savedImage.getId());

        return GalleryImageDto.fromEntity(savedImage);
    }

    @Transactional
    @SuppressWarnings("null")
    public GalleryImageDto updateImage(Long id, String caption, String locationTag, String[] tags) {
        GalleryImage image = galleryImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gallery image not found with id: " + id));

        if (caption != null) {
            image.setCaption(caption);
        }
        if (locationTag != null) {
            image.setLocationTag(locationTag);
        }
        if (tags != null) {
            image.setTags(tags);
        }

        GalleryImage updatedImage = galleryImageRepository.save(image);
        return GalleryImageDto.fromEntity(updatedImage);
    }

    @Transactional
    @SuppressWarnings("null")
    public void deleteImage(Long id) {
        GalleryImage image = galleryImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gallery image not found with id: " + id));

        // Delete physical file
        if (image.getImagePath() != null) {
            fileStorageService.deleteFile(image.getImagePath());
        }
        if (image.getThumbnailPath() != null) {
            fileStorageService.deleteFile(image.getThumbnailPath());
        }

        galleryImageRepository.delete(image);
        logger.info("Gallery image deleted: {}", id);
    }

    /**
     * Creates gallery images from site report photos.
     * Called automatically when a site report is created with photos.
     */
    @Transactional
    @SuppressWarnings("null")
    public void createImagesFromSiteReport(Long siteReportId, PortalUser uploadedBy) {
        SiteReport siteReport = siteReportRepository.findById(siteReportId)
                .orElseThrow(() -> new RuntimeException("Site report not found: " + siteReportId));

        if (siteReport.getPhotos() == null || siteReport.getPhotos().isEmpty()) {
            return;
        }

        for (var photo : siteReport.getPhotos()) {
            GalleryImage galleryImage = new GalleryImage();
            galleryImage.setProject(siteReport.getProject());
            galleryImage.setSiteReport(siteReport);
            galleryImage.setImagePath(photo.getStoragePath());
            galleryImage.setImageUrl(photo.getPhotoUrl());
            galleryImage.setCaption("From Site Report: " + siteReport.getTitle());
            galleryImage.setTakenDate(siteReport.getReportDate() != null
                    ? siteReport.getReportDate().toLocalDate()
                    : LocalDate.now());
            galleryImage.setUploadedBy(uploadedBy);
            galleryImage.setUploadedAt(LocalDateTime.now());

            galleryImageRepository.save(galleryImage);
        }

        logger.info("Created {} gallery images from site report {}",
                siteReport.getPhotos().size(), siteReportId);
    }

    @Transactional(readOnly = true)
    public long getImageCountByProject(Long projectId) {
        return galleryImageRepository.countByProjectId(projectId);
    }
}
