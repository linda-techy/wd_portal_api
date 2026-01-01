package com.wd.api.repository;

import com.wd.api.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Long>, JpaSpecificationExecutor<PaymentTransaction> {

    List<PaymentTransaction> findByScheduleIdOrderByPaymentDateDesc(Long scheduleId);

    List<PaymentTransaction> findByScheduleId(Long scheduleId);

    Optional<PaymentTransaction> findTopByOrderByIdDesc();

    // Database function for safe receipt number generation (fixes race condition)
    @org.springframework.data.jpa.repository.Query(value = "SELECT generate_receipt_number()", nativeQuery = true)
    String generateReceiptNumber();
}
