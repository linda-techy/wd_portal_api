package com.wd.api.repository;

import com.wd.api.model.LeadQuotationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadQuotationItemRepository extends JpaRepository<LeadQuotationItem, Long> {
}
