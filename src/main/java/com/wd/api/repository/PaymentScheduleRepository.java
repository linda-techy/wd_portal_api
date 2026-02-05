package com.wd.api.repository;

import com.wd.api.model.PaymentSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {

    List<PaymentSchedule> findByDesignPaymentIdOrderByInstallmentNumberAsc(Long designPaymentId);

    List<PaymentSchedule> findByStatus(String status);

    List<PaymentSchedule> findByDesignPayment_Project_Id(Long projectId);

    /**
     * Find payment schedules for multiple projects (used by customer API)
     */
    @Query("SELECT ps FROM PaymentSchedule ps WHERE ps.designPayment.project.id IN :projectIds")
    Page<PaymentSchedule> findByProjectIdIn(@Param("projectIds") List<Long> projectIds, Pageable pageable);
}
