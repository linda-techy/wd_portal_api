package com.wd.api.repository;

import com.wd.api.model.FeedbackResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackResponseRepository extends JpaRepository<FeedbackResponse, Long> {
    
    List<FeedbackResponse> findByFormIdOrderBySubmittedAtDesc(Long formId);
    
    List<FeedbackResponse> findByProjectIdOrderBySubmittedAtDesc(Long projectId);
    
    List<FeedbackResponse> findByCustomerIdOrderBySubmittedAtDesc(Long customerId);
    
    Optional<FeedbackResponse> findByFormIdAndCustomerId(Long formId, Long customerId);
    
    @Query("SELECT COUNT(r) FROM FeedbackResponse r WHERE r.form.id = :formId")
    Long countByFormId(@Param("formId") Long formId);
    
    @Query("SELECT AVG(CAST(SUBSTRING(r.responseData, " +
           "POSITION('\"rating\":' IN r.responseData) + 9, 1) AS integer)) " +
           "FROM FeedbackResponse r WHERE r.form.id = :formId AND r.responseData LIKE '%rating%'")
    Double getAverageRatingByFormId(@Param("formId") Long formId);
    
    boolean existsByFormIdAndCustomerId(Long formId, Long customerId);
}
