package com.wd.api.repository;

import com.wd.api.model.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findByApproverIdAndStatus(Long approverId, String status);

    List<ApprovalRequest> findByRequestedById(Long requestedById);

    List<ApprovalRequest> findByTargetTypeAndTargetId(String targetType, Long targetId);
}
