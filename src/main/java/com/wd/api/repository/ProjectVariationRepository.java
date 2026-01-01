package com.wd.api.repository;

import com.wd.api.model.ProjectVariation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectVariationRepository extends JpaRepository<ProjectVariation, Long> {
    List<ProjectVariation> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ProjectVariation> findByProjectIdAndStatus(Long projectId, String status);
}
