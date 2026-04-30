package com.wd.api.repository;

import com.wd.api.model.QuotationAssumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationAssumptionRepository extends JpaRepository<QuotationAssumption, Long> {
    List<QuotationAssumption> findByQuotationIdOrderByDisplayOrderAsc(Long quotationId);

    void deleteByQuotationId(Long quotationId);
}
