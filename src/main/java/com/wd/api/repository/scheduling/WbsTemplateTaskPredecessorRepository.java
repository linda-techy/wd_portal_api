package com.wd.api.repository.scheduling;

import com.wd.api.model.scheduling.WbsTemplateTaskPredecessor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WbsTemplateTaskPredecessorRepository
        extends JpaRepository<WbsTemplateTaskPredecessor, Long> {

    @Query("SELECT p FROM WbsTemplateTaskPredecessor p " +
           "WHERE p.successor.phase.template.id = :templateId")
    List<WbsTemplateTaskPredecessor> findAllForTemplate(@Param("templateId") Long templateId);

    List<WbsTemplateTaskPredecessor> findBySuccessorId(Long successorId);
}
