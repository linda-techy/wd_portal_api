package com.wd.api.repository;

import com.wd.api.model.QuotationViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationViewLogRepository extends JpaRepository<QuotationViewLog, Long> {

    List<QuotationViewLog> findByQuotationIdOrderByViewedAtDesc(Long quotationId);

    long countByQuotationId(Long quotationId);
}
