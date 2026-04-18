package com.wd.api.repository;

import com.wd.api.model.CctvCamera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CctvCameraRepository extends JpaRepository<CctvCamera, Long> {

    List<CctvCamera> findByProjectIdOrderByDisplayOrder(Long projectId);

    List<CctvCamera> findByProjectIdAndIsActiveTrueOrderByDisplayOrder(Long projectId);
}
