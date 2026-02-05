package com.wd.api.repository;

import com.wd.api.model.FeedbackForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackFormRepository extends JpaRepository<FeedbackForm, Long>, JpaSpecificationExecutor<FeedbackForm> {
    
    List<FeedbackForm> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    
    List<FeedbackForm> findByProjectIdAndIsActiveTrueOrderByCreatedAtDesc(Long projectId);
    
    @Query("SELECT f FROM FeedbackForm f WHERE f.project.id = :projectId AND f.isActive = true ORDER BY f.createdAt DESC")
    List<FeedbackForm> findActiveFormsByProject(@Param("projectId") Long projectId);
    
    @Query("SELECT COUNT(f) FROM FeedbackForm f WHERE f.project.id = :projectId AND f.isActive = true")
    Long countActiveFormsByProject(@Param("projectId") Long projectId);
    
    List<FeedbackForm> findByCreatedByIdOrderByCreatedAtDesc(Long createdById);
}
