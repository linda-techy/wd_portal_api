package com.wd.api.repository;

import com.wd.api.model.ProjectWarranty;
import com.wd.api.model.enums.WarrantyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectWarrantyRepository extends JpaRepository<ProjectWarranty, Long> {
    List<ProjectWarranty> findByProjectId(Long projectId);

    List<ProjectWarranty> findByProjectIdAndStatus(Long projectId, WarrantyStatus status);
}
