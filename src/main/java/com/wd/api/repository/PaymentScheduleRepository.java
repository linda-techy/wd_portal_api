package com.wd.api.repository;

import com.wd.api.model.PaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {

    List<PaymentSchedule> findByDesignPaymentIdOrderByInstallmentNumberAsc(Long designPaymentId);

    List<PaymentSchedule> findByStatus(String status);
}
