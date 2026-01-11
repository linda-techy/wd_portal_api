package com.wd.api.repository;

import com.wd.api.model.RetentionRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RetentionReleaseRepository extends JpaRepository<RetentionRelease, Long> {
    List<RetentionRelease> findByWorkOrderId(Long workOrderId);
}
