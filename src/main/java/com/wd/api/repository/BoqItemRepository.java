package com.wd.api.repository;

import com.wd.api.model.BoqItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BoqItemRepository extends JpaRepository<BoqItem, Long> {
    List<BoqItem> findByProjectId(Long projectId);

    List<BoqItem> findByProjectIdAndWorkTypeId(Long projectId, Long workTypeId);

    List<BoqItem> findByProjectIdAndMaterialId(Long projectId, Long materialId);
}
