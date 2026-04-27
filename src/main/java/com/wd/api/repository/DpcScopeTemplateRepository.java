package com.wd.api.repository;

import com.wd.api.model.DpcScopeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DpcScopeTemplateRepository extends JpaRepository<DpcScopeTemplate, Long> {

    Optional<DpcScopeTemplate> findByCode(String code);

    List<DpcScopeTemplate> findByIsActiveTrueOrderByDisplayOrderAsc();
}
