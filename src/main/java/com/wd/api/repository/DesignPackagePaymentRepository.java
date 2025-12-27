package com.wd.api.repository;

import com.wd.api.model.DesignPackagePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DesignPackagePaymentRepository
        extends JpaRepository<DesignPackagePayment, Long>, JpaSpecificationExecutor<DesignPackagePayment> {

    Optional<DesignPackagePayment> findByProject_Id(Long projectId);

    boolean existsByProject_Id(Long projectId);
}
