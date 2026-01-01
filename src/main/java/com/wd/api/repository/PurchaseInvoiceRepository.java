package com.wd.api.repository;

import com.wd.api.model.PurchaseInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PurchaseInvoiceRepository extends JpaRepository<PurchaseInvoice, Long> {
    List<PurchaseInvoice> findByProjectId(Long projectId);

    List<PurchaseInvoice> findByVendorId(Long vendorId);

    // Payment status queries
    List<PurchaseInvoice> findByPaymentStatusIn(List<String> statuses);

    List<PurchaseInvoice> findByPaymentStatus(String paymentStatus);
}
