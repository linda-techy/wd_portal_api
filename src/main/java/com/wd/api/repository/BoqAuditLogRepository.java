package com.wd.api.repository;

import com.wd.api.model.BoqAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoqAuditLogRepository extends JpaRepository<BoqAuditLog, Long> {

    List<BoqAuditLog> findByEntityTypeAndEntityIdOrderByChangedAtDesc(String entityType, Long entityId);

    List<BoqAuditLog> findByProjectIdOrderByChangedAtDesc(Long projectId);
}
