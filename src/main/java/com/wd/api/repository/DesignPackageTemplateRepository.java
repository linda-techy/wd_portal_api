package com.wd.api.repository;

import com.wd.api.model.DesignPackageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignPackageTemplateRepository
        extends JpaRepository<DesignPackageTemplate, Long> {

    /** Active templates ordered for the customer-facing tier picker. */
    List<DesignPackageTemplate> findByIsActiveTrueOrderByDisplayOrderAscIdAsc();

    /** Admin list — includes archived rows so staff can re-activate. */
    List<DesignPackageTemplate> findAllByOrderByDisplayOrderAscIdAsc();

    Optional<DesignPackageTemplate> findByCodeIgnoreCase(String code);
}
