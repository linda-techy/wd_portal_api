package com.wd.api.repository;

import com.wd.api.model.ProjectPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectPhaseRepository extends JpaRepository<ProjectPhase, Long> {
    List<ProjectPhase> findByProjectIdOrderByDisplayOrderAsc(Long projectId);

    List<ProjectPhase> findByProjectIdAndStatus(Long projectId, String status);
}
