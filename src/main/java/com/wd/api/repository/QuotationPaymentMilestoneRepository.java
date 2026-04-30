package com.wd.api.repository;

import com.wd.api.model.QuotationPaymentMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationPaymentMilestoneRepository
        extends JpaRepository<QuotationPaymentMilestone, Long> {

    List<QuotationPaymentMilestone> findByQuotationIdOrderByMilestoneNumberAsc(Long quotationId);

    void deleteByQuotationId(Long quotationId);
}
