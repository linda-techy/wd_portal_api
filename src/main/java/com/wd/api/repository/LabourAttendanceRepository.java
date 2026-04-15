package com.wd.api.repository;

import com.wd.api.model.LabourAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LabourAttendanceRepository extends JpaRepository<LabourAttendance, Long> {
    List<LabourAttendance> findByProjectIdAndAttendanceDate(Long projectId, LocalDate date);

    List<LabourAttendance> findByProjectIdAndAttendanceDateBetween(Long projectId, LocalDate startDate,
            LocalDate endDate);

    /** Count all labourers marked PRESENT on a given date across all projects. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COUNT(la) FROM LabourAttendance la " +
            "WHERE la.attendanceDate = :date AND la.status = com.wd.api.model.enums.AttendanceStatus.PRESENT")
    long countPresentOnDate(@org.springframework.data.repository.query.Param("date") LocalDate date);
}
