package com.wd.api.repository;

import com.wd.api.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findByProjectId(Long projectId);

    List<Receipt> findByInvoiceId(Long invoiceId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Receipt r WHERE r.invoice.id = :invoiceId")
    BigDecimal sumAmountByInvoiceId(@Param("invoiceId") Long invoiceId);
}
