package com.wd.api.repository;

import com.wd.api.model.ProjectInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectInvoiceRepository extends JpaRepository<ProjectInvoice, Long> {
    List<ProjectInvoice> findByProjectId(Long projectId);

    /** Sum of all issued invoices (revenue invoiced). */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COALESCE(SUM(i.totalAmount), 0) FROM ProjectInvoice i WHERE i.status = 'ISSUED'")
    java.math.BigDecimal sumIssuedAmount();

    /** Sum of invoices for a specific project (for FinancialSnapshot). */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COALESCE(SUM(i.totalAmount), 0) FROM ProjectInvoice i " +
            "WHERE i.project.id = :projectId AND i.status != 'CANCELLED'")
    java.math.BigDecimal sumByProjectId(@org.springframework.data.repository.query.Param("projectId") Long projectId);

    /** Monthly invoiced amount trend. */
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT TO_CHAR(i.invoice_date, 'YYYY-MM') AS month, SUM(i.total_amount) AS total " +
                    "FROM project_invoices i WHERE i.status = 'ISSUED' AND i.invoice_date >= :fromDate " +
                    "GROUP BY TO_CHAR(i.invoice_date, 'YYYY-MM') ORDER BY month",
            nativeQuery = true)
    List<Object[]> monthlyInvoicedAmount(@org.springframework.data.repository.query.Param("fromDate") java.time.LocalDate fromDate);
}
