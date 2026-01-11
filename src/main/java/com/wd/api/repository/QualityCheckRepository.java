package com.wd.api.repository;

import com.wd.api.model.QualityCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QualityCheckRepository extends JpaRepository<QualityCheck, Long> {
    List<QualityCheck> findByProjectId(Long projectId);

    List<QualityCheck> findByConductedById(Long userId);
}
