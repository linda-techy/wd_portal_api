package com.wd.api.repository;

import com.wd.api.model.scheduling.Holiday;
import com.wd.api.model.scheduling.HolidayScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    @Query("""
        SELECT h FROM Holiday h
        WHERE h.scope = :scope
          AND (:scopeRef IS NULL OR h.scopeRef = :scopeRef)
          AND h.date BETWEEN :start AND :end
          AND h.active = true
        """)
    List<Holiday> findByScopeAndDateRange(
            @Param("scope") HolidayScope scope,
            @Param("scopeRef") String scopeRef,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("""
        SELECT h FROM Holiday h
        WHERE h.code = :code
          AND h.date = :date
          AND h.scope = :scope
          AND ((h.scopeRef IS NULL AND :scopeRef IS NULL) OR h.scopeRef = :scopeRef)
        """)
    Optional<Holiday> findByDedupeKey(
            @Param("code") String code,
            @Param("date") LocalDate date,
            @Param("scope") HolidayScope scope,
            @Param("scopeRef") String scopeRef);

    List<Holiday> findByScopeAndDate_Year(HolidayScope scope, int year);
}
