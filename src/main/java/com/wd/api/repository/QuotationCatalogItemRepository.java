package com.wd.api.repository;

import com.wd.api.model.QuotationCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuotationCatalogItemRepository
        extends JpaRepository<QuotationCatalogItem, Long>,
                JpaSpecificationExecutor<QuotationCatalogItem> {

    Optional<QuotationCatalogItem> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Modifying
    @Query("UPDATE QuotationCatalogItem c SET c.timesUsed = c.timesUsed + 1 WHERE c.id = :id")
    int incrementTimesUsed(@Param("id") Long id);
}
