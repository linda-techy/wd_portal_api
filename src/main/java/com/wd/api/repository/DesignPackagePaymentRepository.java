package com.wd.api.repository;

import com.wd.api.model.DesignPackagePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DesignPackagePaymentRepository
        extends JpaRepository<DesignPackagePayment, Long>, JpaSpecificationExecutor<DesignPackagePayment> {

    Optional<DesignPackagePayment> findByProject_Id(Long projectId);

    boolean existsByProject_Id(Long projectId);

    /** Find payments where DLP has ended and retention has not yet been released. */
    @Query("SELECT d FROM DesignPackagePayment d " +
           "WHERE d.defectLiabilityEndDate IS NOT NULL " +
           "AND d.defectLiabilityEndDate <= :today " +
           "AND d.retentionStatus = 'ACTIVE'")
    List<DesignPackagePayment> findEligibleForRetentionRelease(@Param("today") LocalDate today);
}
