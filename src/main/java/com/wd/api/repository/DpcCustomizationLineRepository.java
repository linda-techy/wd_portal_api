package com.wd.api.repository;

import com.wd.api.model.DpcCustomizationLine;
import com.wd.api.model.enums.DpcCustomizationSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DpcCustomizationLineRepository extends JpaRepository<DpcCustomizationLine, Long> {

    List<DpcCustomizationLine> findByDpcDocumentIdOrderByDisplayOrderAsc(Long dpcDocumentId);

    /**
     * Wipes all customization lines from a DPC document that share the given source.
     * Typically called with {@link DpcCustomizationSource#AUTO_FROM_BOQ_ADDON} to
     * regenerate the auto-rows from the current BoQ snapshot while leaving MANUAL
     * lines untouched.
     */
    @Modifying
    @Transactional
    void deleteByDpcDocumentIdAndSource(Long dpcDocumentId, DpcCustomizationSource source);
}
