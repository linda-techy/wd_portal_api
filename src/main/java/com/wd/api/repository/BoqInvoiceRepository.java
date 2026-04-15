package com.wd.api.repository;

import com.wd.api.model.BoqInvoice;
import com.wd.api.model.enums.ProjectInvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoqInvoiceRepository extends JpaRepository<BoqInvoice, Long> {

    List<BoqInvoice> findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long projectId);

    List<BoqInvoice> findByProjectIdAndInvoiceTypeAndDeletedAtIsNull(Long projectId, String invoiceType);

    Optional<BoqInvoice> findByProjectIdAndInvoiceNumber(Long projectId, String invoiceNumber);

    Optional<BoqInvoice> findByPaymentStageIdAndDeletedAtIsNull(Long paymentStageId);

    List<BoqInvoice> findByChangeOrderIdAndDeletedAtIsNull(Long changeOrderId);

    /** Count invoices for a project to generate the next invoice number. */
    long countByProjectIdAndDeletedAtIsNull(Long projectId);

    /** Sum of net_amount_due for all non-void, non-paid stage invoices on this project. */
    @Query("SELECT COALESCE(SUM(i.netAmountDue), 0) FROM BoqInvoice i " +
           "WHERE i.project.id = :projectId AND i.invoiceType = 'STAGE_INVOICE' " +
           "AND i.status NOT IN ('PAID','VOID') AND i.deletedAt IS NULL")
    BigDecimal sumOutstandingStageInvoices(@Param("projectId") Long projectId);

    /** Sum of total_incl_gst for all PAID invoices on this project. */
    @Query("SELECT COALESCE(SUM(i.totalInclGst), 0) FROM BoqInvoice i " +
           "WHERE i.project.id = :projectId AND i.status = 'PAID' AND i.deletedAt IS NULL")
    BigDecimal sumTotalCollected(@Param("projectId") Long projectId);

    List<BoqInvoice> findByProjectIdAndStatusAndDeletedAtIsNull(Long projectId, ProjectInvoiceStatus status);
}
