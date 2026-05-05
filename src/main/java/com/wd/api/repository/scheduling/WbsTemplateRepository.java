package com.wd.api.repository.scheduling;

import com.wd.api.model.scheduling.WbsTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WbsTemplateRepository extends JpaRepository<WbsTemplate, Long> {

    Optional<WbsTemplate> findByCodeAndIsActiveTrue(String code);

    Optional<WbsTemplate> findByCodeAndVersion(String code, Integer version);

    @Query("SELECT MAX(t.version) FROM WbsTemplate t WHERE t.code = :code")
    Optional<Integer> findMaxVersionForCode(@Param("code") String code);

    List<WbsTemplate> findByCodeOrderByVersionDesc(String code);

    List<WbsTemplate> findByIsActiveTrue();
}
