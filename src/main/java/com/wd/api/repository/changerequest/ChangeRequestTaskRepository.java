package com.wd.api.repository.changerequest;

import com.wd.api.model.changerequest.ChangeRequestTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeRequestTaskRepository extends JpaRepository<ChangeRequestTask, Long> {

    /**
     * All proposed tasks for a CR ordered by sequence ascending. Used by
     * ChangeRequestMergeService to clone in the same order the author wrote.
     */
    List<ChangeRequestTask> findByChangeRequestIdOrderBySequenceAsc(Long changeRequestId);

    /**
     * Lightweight membership check for predecessor add — confirms both
     * endpoints belong to the same CR.
     */
    boolean existsByIdAndChangeRequestId(Long id, Long changeRequestId);

    /**
     * Counts proposed tasks for sequence-allocation when a new task is appended.
     */
    long countByChangeRequestId(Long changeRequestId);
}
