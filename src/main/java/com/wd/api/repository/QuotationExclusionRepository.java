package com.wd.api.repository;

import com.wd.api.model.QuotationExclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationExclusionRepository extends JpaRepository<QuotationExclusion, Long> {
    List<QuotationExclusion> findByQuotationIdOrderByDisplayOrderAsc(Long quotationId);

    void deleteByQuotationId(Long quotationId);
}
