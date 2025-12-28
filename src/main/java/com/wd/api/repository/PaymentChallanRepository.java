package com.wd.api.repository;

import com.wd.api.model.PaymentChallan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentChallanRepository
        extends JpaRepository<PaymentChallan, Long>, JpaSpecificationExecutor<PaymentChallan> {
    Optional<PaymentChallan> findByTransactionId(Long transactionId);

    boolean existsByTransactionId(Long transactionId);
}
