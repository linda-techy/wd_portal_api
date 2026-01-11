package com.wd.api.repository;

import com.wd.api.model.WageSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WageSheetRepository extends JpaRepository<WageSheet, Long> {
    Optional<WageSheet> findBySheetNumber(String sheetNumber);
}
