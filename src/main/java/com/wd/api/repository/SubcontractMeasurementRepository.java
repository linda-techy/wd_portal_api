package com.wd.api.repository;

import com.wd.api.model.SubcontractMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcontractMeasurementRepository extends JpaRepository<SubcontractMeasurement, Long> {
        List<SubcontractMeasurement> findByWorkOrderId(Long workOrderId);
}
