package com.wd.api.repository;

import com.wd.api.model.LabourAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LabourAttendanceRepository extends JpaRepository<LabourAttendance, Long> {
    List<LabourAttendance> findByProjectIdAndAttendanceDate(Long projectId, LocalDate date);
}
