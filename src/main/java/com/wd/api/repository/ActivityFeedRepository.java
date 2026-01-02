package com.wd.api.repository;

import com.wd.api.model.ActivityFeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityFeedRepository extends JpaRepository<ActivityFeed, Long> {
    List<ActivityFeed> findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(Long referenceId, String referenceType);

    List<ActivityFeed> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ActivityFeed> findTop10ByProjectIdOrderByCreatedAtDesc(Long projectId);
}
