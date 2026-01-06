package com.wd.api.repository;

import com.wd.api.model.View360;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface View360Repository extends JpaRepository<View360, Long> {
    List<View360> findByProjectIdOrderByCaptureDateDesc(Long projectId);
}
