package com.wd.api.repository;

import com.wd.api.model.SubcontractWorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcontractWorkOrderRepository extends JpaRepository<SubcontractWorkOrder, Long>, JpaSpecificationExecutor<SubcontractWorkOrder> {
    List<SubcontractWorkOrder> findByProjectId(Long projectId);

    boolean existsByWorkOrderNumber(String workOrderNumber);
}
