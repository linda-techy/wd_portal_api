package com.wd.api.service;

import com.wd.api.dto.QualityCheckSearchFilter;
import com.wd.api.model.QualityCheck;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.QualityCheckRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.util.SpecificationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class QualityCheckService {

    @Autowired
    private QualityCheckRepository qualityCheckRepository;

    @Autowired
    private CustomerProjectRepository projectRepository;

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Transactional(readOnly = true)
    public Page<QualityCheck> searchQualityChecks(QualityCheckSearchFilter filter) {
        Specification<QualityCheck> spec = buildSpecification(filter);
        return qualityCheckRepository.findAll(spec, filter.toPageable());
    }

    private Specification<QualityCheck> buildSpecification(QualityCheckSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search across title, description, inspector name
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), searchPattern),
                    cb.like(cb.lower(root.get("description")), searchPattern),
                    cb.like(cb.lower(root.join("conductedBy").get("name")), searchPattern)
                ));
            }

            // Filter by projectId
            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            }

            // Filter by inspectorId (conductedBy)
            if (filter.getInspectorId() != null) {
                predicates.add(cb.equal(root.get("conductedBy").get("id"), filter.getInspectorId()));
            }

            // Filter by checklistName (title)
            if (filter.getChecklistName() != null && !filter.getChecklistName().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + filter.getChecklistName().toLowerCase() + "%"));
            }

            // Filter by result
            if (filter.getResult() != null && !filter.getResult().isEmpty()) {
                predicates.add(cb.equal(root.get("result"), filter.getResult()));
            }

            // Filter by status
            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            // Date range filter
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkDate"), filter.getStartDate().atStartOfDay()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("checkDate"), filter.getEndDate().atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public List<QualityCheck> getProjectChecks(Long projectId) {
        return qualityCheckRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public QualityCheck getCheckById(Long id) {
        return qualityCheckRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quality check not found with id: " + id));
    }

    @Transactional
    public QualityCheck createCheck(QualityCheck check, Long projectId, Long conductedById) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        PortalUser conductedBy = portalUserRepository.findById(conductedById)
                .orElseThrow(() -> new RuntimeException("User not found"));

        check.setProject(project);
        check.setConductedBy(conductedBy);

        if (check.getCheckDate() == null) {
            check.setCheckDate(LocalDateTime.now());
        }

        return qualityCheckRepository.save(check);
    }

    @Transactional
    public QualityCheck updateCheck(Long id, QualityCheck checkDetails) {
        QualityCheck check = getCheckById(id);

        check.setTitle(checkDetails.getTitle());
        check.setDescription(checkDetails.getDescription());
        check.setStatus(checkDetails.getStatus());
        check.setResult(checkDetails.getResult());
        check.setRemarks(checkDetails.getRemarks());

        return qualityCheckRepository.save(check);
    }

    @Transactional
    public void deleteCheck(Long id) {
        qualityCheckRepository.deleteById(id);
    }
}
