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

    // ===== Dashboard Aggregations =====

    /** Total revenue actually collected across all projects. */
    @Query("SELECT COALESCE(SUM(ps.paidAmount), 0) FROM PaymentSchedule ps WHERE ps.status = 'PAID'")
    java.math.BigDecimal sumPaidAmount();

    /** Revenue target = total contract value of all active projects' payment packages. */
    @Query("SELECT COALESCE(SUM(dp.totalAmount), 0) FROM DesignPackagePayment dp " +
           "JOIN dp.project p WHERE p.projectStatus = com.wd.api.model.enums.ProjectStatus.ACTIVE " +
           "AND p.deletedAt IS NULL")
    java.math.BigDecimal sumTargetAmountActiveProjects();

    /** Monthly collected revenue trend for the last N months (native query for date grouping). */
    @Query(value = "SELECT TO_CHAR(ps.paid_date, 'YYYY-MM') AS month, SUM(ps.paid_amount) AS total " +
                   "FROM payment_schedule ps " +
                   "WHERE ps.status = 'PAID' AND ps.paid_date >= :fromDate " +
                   "GROUP BY TO_CHAR(ps.paid_date, 'YYYY-MM') ORDER BY month",
           nativeQuery = true)
    List<Object[]> monthlyRevenueCollected(@Param("fromDate") java.time.LocalDate fromDate);

    /** Count of payment schedules grouped by status (PENDING / PAID / OVERDUE). */
    @Query("SELECT ps.status, COUNT(ps) FROM PaymentSchedule ps GROUP BY ps.status")
    List<Object[]> countByStatus();
}
