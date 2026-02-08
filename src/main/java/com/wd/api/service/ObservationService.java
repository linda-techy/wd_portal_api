package com.wd.api.service;

import com.wd.api.dto.ObservationDto;
import com.wd.api.dto.ObservationSearchFilter;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.Observation;
import com.wd.api.model.PortalUser;
import com.wd.api.model.StaffRole;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.ObservationRepository;
import com.wd.api.repository.StaffRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ObservationService {

    private static final Logger logger = LoggerFactory.getLogger(ObservationService.class);

    private final ObservationRepository observationRepository;
    private final CustomerProjectRepository projectRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final FileStorageService fileStorageService;

    public ObservationService(ObservationRepository observationRepository,
            CustomerProjectRepository projectRepository,
            StaffRoleRepository staffRoleRepository,
            FileStorageService fileStorageService) {
        this.observationRepository = observationRepository;
        this.projectRepository = projectRepository;
        this.staffRoleRepository = staffRoleRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<ObservationDto> searchObservations(ObservationSearchFilter filter) {
        Specification<Observation> spec = buildSpecification(filter);
        Page<Observation> observations = observationRepository.findAll(spec, filter.toPageable());
        return observations.map(ObservationDto::fromEntity);
    }

    private Specification<Observation> buildSpecification(ObservationSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), searchPattern),
                        cb.like(cb.lower(root.get("description")), searchPattern),
                        cb.like(cb.lower(root.get("location")), searchPattern)));
            }

            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            }

            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getPriority() != null && !filter.getPriority().isEmpty()) {
                predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
            }

            if (filter.getSeverity() != null && !filter.getSeverity().isEmpty()) {
                predicates.add(cb.equal(root.get("severity"), filter.getSeverity()));
            }

            if (filter.getReportedById() != null) {
                predicates.add(cb.equal(root.get("reportedBy").get("id"), filter.getReportedById()));
            }

            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("reportedDate"),
                        filter.getStartDate().atStartOfDay()));
            }

            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("reportedDate"),
                        filter.getEndDate().atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public List<ObservationDto> getObservationsByProject(Long projectId) {
        List<Observation> observations = observationRepository.findByProjectIdOrderByReportedDateDesc(projectId);
        return observations.stream()
                .map(ObservationDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ObservationDto> getActiveObservationsByProject(Long projectId) {
        List<Observation> observations = observationRepository.findActiveByProjectId(projectId);
        return observations.stream()
                .map(ObservationDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ObservationDto> getResolvedObservationsByProject(Long projectId) {
        List<Observation> observations = observationRepository.findResolvedByProjectId(projectId);
        return observations.stream()
                .map(ObservationDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public ObservationDto getObservationById(Long id) {
        Observation observation = observationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Observation not found with id: " + id));
        return ObservationDto.fromEntity(observation);
    }

    @Transactional
    @SuppressWarnings("null")
    public ObservationDto createObservation(Long projectId, String title, String description,
            String location, String priority, String severity,
            MultipartFile image, PortalUser reportedBy, Long reportedByRoleId) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        Observation observation = new Observation();
        observation.setProject(project);
        observation.setTitle(title);
        observation.setDescription(description);
        observation.setLocation(location);
        observation.setPriority(priority != null ? priority : "MEDIUM");
        observation.setSeverity(severity != null ? severity : "MEDIUM");
        observation.setStatus("OPEN");
        observation.setReportedBy(reportedBy);
        observation.setReportedDate(LocalDateTime.now());

        if (reportedByRoleId != null) {
            StaffRole role = staffRoleRepository.findById(reportedByRoleId).orElse(null);
            observation.setReportedByRole(role);
        }

        if (image != null && !image.isEmpty()) {
            String subDir = "observations/" + projectId;
            String storedPath = fileStorageService.storeFile(image, subDir);
            observation.setImagePath(storedPath);
        }

        Observation savedObservation = observationRepository.save(observation);
        logger.info("Observation created for project {}: {}", projectId, savedObservation.getId());

        return ObservationDto.fromEntity(savedObservation);
    }

    @Transactional
    @SuppressWarnings("null")
    public ObservationDto updateObservation(Long id, String title, String description,
            String location, String priority, String severity,
            String status, MultipartFile image) {
        Observation observation = observationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Observation not found with id: " + id));

        if (title != null)
            observation.setTitle(title);
        if (description != null)
            observation.setDescription(description);
        if (location != null)
            observation.setLocation(location);
        if (priority != null)
            observation.setPriority(priority);
        if (severity != null)
            observation.setSeverity(severity);
        if (status != null)
            observation.setStatus(status);

        if (image != null && !image.isEmpty()) {
            // Delete old image if exists
            if (observation.getImagePath() != null) {
                fileStorageService.deleteFile(observation.getImagePath());
            }
            String subDir = "observations/" + observation.getProject().getId();
            String storedPath = fileStorageService.storeFile(image, subDir);
            observation.setImagePath(storedPath);
        }

        Observation updatedObservation = observationRepository.save(observation);
        return ObservationDto.fromEntity(updatedObservation);
    }

    @Transactional
    @SuppressWarnings("null")
    public ObservationDto resolveObservation(Long id, String resolutionNotes, PortalUser resolvedBy) {
        Observation observation = observationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Observation not found with id: " + id));

        observation.setStatus("RESOLVED");
        observation.setResolutionNotes(resolutionNotes);
        observation.setResolvedDate(LocalDateTime.now());
        observation.setResolvedBy(resolvedBy);

        Observation resolvedObservation = observationRepository.save(observation);
        logger.info("Observation {} resolved by {}", id, resolvedBy.getFirstName() + " " + resolvedBy.getLastName());

        return ObservationDto.fromEntity(resolvedObservation);
    }

    @Transactional
    @SuppressWarnings("null")
    public ObservationDto updateStatus(Long id, String status) {
        Observation observation = observationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Observation not found with id: " + id));

        observation.setStatus(status);
        Observation updatedObservation = observationRepository.save(observation);
        return ObservationDto.fromEntity(updatedObservation);
    }

    @Transactional
    @SuppressWarnings("null")
    public void deleteObservation(Long id) {
        Observation observation = observationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Observation not found with id: " + id));

        // Delete image file if exists
        if (observation.getImagePath() != null) {
            fileStorageService.deleteFile(observation.getImagePath());
        }

        observationRepository.delete(observation);
        logger.info("Observation deleted: {}", id);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getObservationCountsByProject(Long projectId) {
        Map<String, Long> counts = new HashMap<>();
        counts.put("active", observationRepository.countActiveByProjectId(projectId));
        counts.put("resolved", observationRepository.countResolvedByProjectId(projectId));
        counts.put("total", observationRepository.countByProjectId(projectId));
        return counts;
    }

    @Transactional(readOnly = true)
    public List<ObservationDto> getMyObservations(Long userId) {
        List<Observation> observations = observationRepository.findByReportedByIdOrderByReportedDateDesc(userId);
        return observations.stream()
                .map(ObservationDto::fromEntity)
                .collect(Collectors.toList());
    }
}
