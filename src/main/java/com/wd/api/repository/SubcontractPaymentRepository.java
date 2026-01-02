package com.wd.api.repository;

import com.wd.api.model.SubcontractPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcontractPaymentRepository extends JpaRepository<SubcontractPayment, Long> {
        List<SubcontractPayment> findByWorkOrderId(Long workOrderId);
}
