package com.wd.api.repository;

import com.wd.api.model.DelayLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DelayLogRepository extends JpaRepository<DelayLog, Long> {
    List<DelayLog> findByProjectIdOrderByFromDateDesc(Long projectId);

    List<DelayLog> findByProjectIdAndDelayType(Long projectId, String delayType);

    List<DelayLog> findByProjectId(Long projectId);
}
