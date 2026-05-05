package com.wd.api.repository.scheduling;

import com.wd.api.model.scheduling.WbsTemplatePhase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WbsTemplatePhaseRepository extends JpaRepository<WbsTemplatePhase, Long> {
    List<WbsTemplatePhase> findByTemplateIdOrderBySequenceAsc(Long templateId);
}
