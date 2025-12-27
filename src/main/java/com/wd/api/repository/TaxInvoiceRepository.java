package com.wd.api.repository;

import com.wd.api.model.TaxInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxInvoiceRepository extends JpaRepository<TaxInvoice, Long> {

    Optional<TaxInvoice> findByPaymentId(Long paymentId);

    List<TaxInvoice> findByFinancialYearOrderByInvoiceDateDesc(String financialYear);

    @Query(value = "SELECT generate_invoice_number()", nativeQuery = true)
    String generateInvoiceNumber();

    boolean existsByPaymentId(Long paymentId);
}
