package com.wd.api.repository;

import com.wd.api.model.LabourAdvance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LabourAdvanceRepository extends JpaRepository<LabourAdvance, Long> {
    List<LabourAdvance> findByLabourId(Long labourId);
}
