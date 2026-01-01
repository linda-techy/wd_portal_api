package com.wd.api.repository;

import com.wd.api.model.LabourPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LabourPaymentRepository extends JpaRepository<LabourPayment, Long> {
    List<LabourPayment> findByProjectId(Long projectId);

    List<LabourPayment> findByLabourId(Long labourId);
}
