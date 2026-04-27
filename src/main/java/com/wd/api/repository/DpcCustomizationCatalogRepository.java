package com.wd.api.repository;

import com.wd.api.model.DpcCustomizationCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DpcCustomizationCatalogRepository
        extends JpaRepository<DpcCustomizationCatalogItem, Long>,
                JpaSpecificationExecutor<DpcCustomizationCatalogItem> {

    Optional<DpcCustomizationCatalogItem> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Modifying
    @Query("UPDATE DpcCustomizationCatalogItem c SET c.timesUsed = c.timesUsed + 1 WHERE c.id = :id")
    int incrementTimesUsed(@Param("id") Long id);
}
