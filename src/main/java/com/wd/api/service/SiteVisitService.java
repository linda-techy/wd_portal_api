package com.wd.api.service;

import com.wd.api.dto.CheckInRequest;
import com.wd.api.dto.CheckOutRequest;
import com.wd.api.dto.SiteVisitDTO;
import com.wd.api.dto.SiteVisitSearchFilter;
import com.wd.api.exception.ResourceNotFoundException;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.model.SiteVisit;
import com.wd.api.model.enums.VisitStatus;
import com.wd.api.model.enums.VisitType;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.SiteVisitRepository;
import com.wd.api.repository.PortalUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing site visits with check-in/check-out functionality
 */
@Service
public class SiteVisitService {

    private final SiteVisitRepository siteVisitRepository;
    private final CustomerProjectRepository projectRepository;
    private final PortalUserRepository portalUserRepository;

    public SiteVisitService(SiteVisitRepository siteVisitRepository,
            CustomerProjectRepository projectRepository,
            PortalUserRepository portalUserRepository) {
        this.siteVisitRepository = siteVisitRepository;
        this.projectRepository = projectRepository;
        this.portalUserRepository = portalUserRepository;
    }

    /**
     * Search site visits with filters and pagination
     */
    @Transactional(readOnly = true)
    public Page<SiteVisit> searchSiteVisits(SiteVisitSearchFilter filter) {
        Specification<SiteVisit> spec = buildSpecification(filter);
        return siteVisitRepository.findAll(spec, filter.toPageable());
    }

    private Specification<SiteVisit> buildSpecification(SiteVisitSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search across project name, visitor name, notes
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.join("project").get("name")), searchPattern),
                    cb.like(cb.lower(root.join("visitedBy").get("firstName")), searchPattern),
                    cb.like(cb.lower(root.join("visitedBy").get("lastName")), searchPattern),
                    cb.like(cb.lower(root.get("notes")), searchPattern)
                ));
            }

            // Filter by projectId
            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            }

            // Filter by visitedById
            if (filter.getVisitedById() != null) {
                predicates.add(cb.equal(root.get("visitedBy").get("id"), filter.getVisitedById()));
            }

            // Filter by visitType
            if (filter.getVisitType() != null && !filter.getVisitType().isEmpty()) {
                try {
                    VisitType visitType = VisitType.valueOf(filter.getVisitType().toUpperCase());
                    predicates.add(cb.equal(root.get("visitType"), visitType));
                } catch (IllegalArgumentException e) {
                    // Invalid visit type, skip filter
                }
            }

            // Filter by visitStatus or active only
            if (filter.isActiveOnly()) {
                predicates.add(cb.equal(root.get("visitStatus"), VisitStatus.CHECKED_IN));
            } else if (filter.getVisitStatus() != null && !filter.getVisitStatus().isEmpty()) {
                try {
                    VisitStatus visitStatus = VisitStatus.valueOf(filter.getVisitStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("visitStatus"), visitStatus));
                } catch (IllegalArgumentException e) {
                    // Invalid visit status, skip filter
                }
            }

            // Filter by status (from base class)
            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                try {
                    VisitStatus visitStatus = VisitStatus.valueOf(filter.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("visitStatus"), visitStatus));
                } catch (IllegalArgumentException e) {
                    // Invalid status, skip filter
                }
            }

            // Date range filter
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("visitDate"), filter.getStartDate().atStartOfDay()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("visitDate"), filter.getEndDate().atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Check in to a project site
     */
    @Transactional
    public SiteVisitDTO checkIn(CheckInRequest request, Long userId) {
        // Verify user doesn't have an active visit
        siteVisitRepository.findActiveVisitByUser(userId)
                .ifPresent(v -> {
                    throw new IllegalStateException(
                            "Cannot check in: You already have an active visit at project " +
                                    v.getProject().getName() + ". Please check out first.");
                });

        CustomerProject project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        PortalUser user = portalUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Create new visit
        SiteVisit visit = new SiteVisit();
        visit.setProject(project);
        visit.setVisitedBy(user);
        visit.setVisitDate(LocalDateTime.now());
        visit.setNotes(request.getNotes());

        // Parse visit type
        VisitType visitType = VisitType.GENERAL;
        if (request.getVisitType() != null && !request.getVisitType().isEmpty()) {
            try {
                visitType = VisitType.valueOf(request.getVisitType().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Keep default GENERAL
            }
        }
        visit.setVisitType(visitType);

        // Perform check-in
        visit.checkIn(request.getLatitude(), request.getLongitude());

        visit = siteVisitRepository.save(visit);
        return mapToDTO(visit);
    }

    /**
     * Check out from a site visit
     */
    @Transactional
    public SiteVisitDTO checkOut(Long visitId, CheckOutRequest request, Long userId) {
        SiteVisit visit = siteVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));

        // Verify ownership
        if (!visit.getVisitedBy().getId().equals(userId)) {
            throw new IllegalStateException("You can only check out from your own visits");
        }

        // Perform check-out (this validates status and calculates duration)
        visit.checkOut(request.getLatitude(), request.getLongitude(), request.getNotes());

        visit = siteVisitRepository.save(visit);
        return mapToDTO(visit);
    }

    /**
     * Get current active visit for a user
     */
    public SiteVisitDTO getActiveVisitForUser(Long userId) {
        return siteVisitRepository.findActiveVisitByUser(userId)
                .map(this::mapToDTO)
                .orElse(null);
    }

    /**
     * Get all currently active visits (admin view)
     */
    public List<SiteVisitDTO> getAllActiveVisits() {
        return siteVisitRepository.findAllActiveVisits().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get today's visits for a project
     */
    public List<SiteVisitDTO> getTodaysVisitsForProject(Long projectId) {
        return siteVisitRepository.findTodaysVisitsByProject(projectId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get visit history for a project
     */
    public List<SiteVisitDTO> getVisitsByProject(Long projectId) {
        return siteVisitRepository.findByProjectIdOrderByVisitDateDesc(projectId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get visits by project and date range
     */
    public List<SiteVisitDTO> getVisitsByProjectAndDateRange(Long projectId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        return siteVisitRepository.findByProjectAndDateRange(projectId, start, end).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get visits by user and date range
     */
    public List<SiteVisitDTO> getVisitsByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        return siteVisitRepository.findByUserAndDateRange(userId, start, end).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific visit by ID
     */
    public SiteVisitDTO getVisitById(Long id) {
        return siteVisitRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
    }

    /**
     * Cancel a pending visit
     */
    @Transactional
    public void cancelVisit(Long visitId, Long userId) {
        SiteVisit visit = siteVisitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));

        if (visit.getVisitStatus() != VisitStatus.PENDING) {
            throw new IllegalStateException("Can only cancel pending visits");
        }

        visit.setVisitStatus(VisitStatus.CANCELLED);
        siteVisitRepository.save(visit);
    }

    /**
     * Get full name from PortalUser
     */
    private String getFullName(PortalUser user) {
        if (user == null)
            return null;
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }

    /**
     * Map entity to DTO
     */
    private SiteVisitDTO mapToDTO(SiteVisit visit) {
        PortalUser visitedBy = visit.getVisitedBy();
        return SiteVisitDTO.builder()
                .id(visit.getId())
                .projectId(visit.getProject().getId())
                .projectName(visit.getProject().getName())
                .visitedById(visitedBy != null ? visitedBy.getId() : null)
                .visitedByName(getFullName(visitedBy))
                .visitDate(visit.getVisitDate())
                .notes(visit.getNotes())
                .checkInTime(visit.getCheckInTime())
                .checkOutTime(visit.getCheckOutTime())
                .checkInLatitude(visit.getCheckInLatitude())
                .checkInLongitude(visit.getCheckInLongitude())
                .checkOutLatitude(visit.getCheckOutLatitude())
                .checkOutLongitude(visit.getCheckOutLongitude())
                .visitType(visit.getVisitType() != null ? visit.getVisitType().name() : null)
                .visitStatus(visit.getVisitStatus() != null ? visit.getVisitStatus().name() : null)
                .durationMinutes(visit.getDurationMinutes())
                .formattedDuration(visit.getFormattedDuration())
                .checkOutNotes(visit.getCheckOutNotes())
                .createdAt(visit.getCreatedAt())
                .build();
    }
}
