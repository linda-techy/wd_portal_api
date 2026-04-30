package com.wd.api.repository;

import com.wd.api.model.QuotationInclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationInclusionRepository extends JpaRepository<QuotationInclusion, Long> {
    List<QuotationInclusion> findByQuotationIdOrderByDisplayOrderAsc(Long quotationId);

    void deleteByQuotationId(Long quotationId);
}
