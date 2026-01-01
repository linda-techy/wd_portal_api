package com.wd.api.repository;

import com.wd.api.model.VendorPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface VendorPaymentRepository extends JpaRepository<VendorPayment, Long> {

    // Find by invoice
    List<VendorPayment> findByInvoiceId(Long invoiceId);

    // Find by payment date range
    List<VendorPayment> findByPaymentDateBetween(LocalDate start, LocalDate end);

    // Find by payment mode
    List<VendorPayment> findByPaymentMode(VendorPayment.PaymentMode paymentMode);

    // Find by vendor (via invoice)
    @Query("SELECT vp FROM VendorPayment vp WHERE vp.invoice.vendor.id = :vendorId ORDER BY vp.paymentDate DESC")
    List<VendorPayment> findByVendorId(@Param("vendorId") Long vendorId);

    // Get total paid for an invoice
    @Query("SELECT COALESCE(SUM(vp.amountPaid), 0) FROM VendorPayment vp WHERE vp.invoice.id = :invoiceId")
    BigDecimal getTotalPaidForInvoice(@Param("invoiceId") Long invoiceId);

    // Get total TDS deducted for a vendor
    @Query("SELECT COALESCE(SUM(vp.tdsDeducted), 0) FROM VendorPayment vp " +
            "WHERE vp.invoice.vendor.id = :vendorId AND vp.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalTdsForVendor(@Param("vendorId") Long vendorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Get payments for a project (via PO → Invoice)
    @Query("SELECT vp FROM VendorPayment vp " +
            "JOIN vp.invoice pi " +
            "JOIN PurchaseOrder po ON po.id = pi.poId " +
            "WHERE po.project.id = :projectId " +
            "ORDER BY vp.paymentDate DESC")
    List<VendorPayment> findByProjectId(@Param("projectId") Long projectId);

    // Count payments for vendor
    long countByInvoice_VendorId(Long vendorId);

    // Recent payments
    @Query("SELECT vp FROM VendorPayment vp ORDER BY vp.createdAt DESC")
    List<VendorPayment> findRecentPayments();
}
