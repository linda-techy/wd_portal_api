package com.wd.api.repository;

import com.wd.api.model.ProjectVariation;
import com.wd.api.model.enums.VariationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectVariationRepository extends JpaRepository<ProjectVariation, Long>, JpaSpecificationExecutor<ProjectVariation> {
    List<ProjectVariation> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ProjectVariation> findByProjectIdAndStatus(Long projectId, VariationStatus status);

    List<ProjectVariation> findByProjectId(Long projectId);
}
