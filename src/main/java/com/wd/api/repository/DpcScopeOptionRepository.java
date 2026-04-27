package com.wd.api.repository;

import com.wd.api.model.DpcScopeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DpcScopeOptionRepository extends JpaRepository<DpcScopeOption, Long> {

    List<DpcScopeOption> findByScopeTemplateIdOrderByDisplayOrderAsc(Long scopeTemplateId);
}
