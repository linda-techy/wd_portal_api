package com.wd.api.service;

import com.wd.api.model.PaymentStage;
import com.wd.api.model.enums.PaymentStageStatus;
import com.wd.api.repository.PaymentStageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class PaymentStageService {

    private final PaymentStageRepository stageRepository;

    public PaymentStageService(PaymentStageRepository stageRepository) {
        this.stageRepository = stageRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentStage> getStagesByDocument(Long boqDocumentId) {
        return stageRepository.findByBoqDocumentIdOrderByStageNumberAsc(boqDocumentId);
    }

    @Transactional(readOnly = true)
    public List<PaymentStage> getStagesByProject(Long projectId) {
        return stageRepository.findByProjectIdOrderByStageNumberAsc(projectId);
    }

    /** Marks a stage as DUE and optionally sets a due date. */
    public PaymentStage markDue(Long stageId, LocalDate dueDate, Long userId) {
        PaymentStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + stageId));
        if (stage.getStatus() != PaymentStageStatus.UPCOMING) {
            throw new IllegalStateException("Stage must be UPCOMING to mark DUE. Current: " + stage.getStatus());
        }
        stage.setStatus(PaymentStageStatus.DUE);
        if (dueDate != null) stage.setDueDate(dueDate);
        stage.setUpdatedByUserId(userId);
        return stageRepository.save(stage);
    }

    /** Marks overdue stages (batch job / scheduled task can call this). */
    public int markOverdueStages() {
        List<PaymentStage> due = stageRepository.findByProjectIdAndStatusOrderByStageNumberAsc(
                0L, PaymentStageStatus.DUE);
        // In production this would use a @Query — simplified for now
        int count = 0;
        LocalDate today = LocalDate.now();
        for (PaymentStage stage : due) {
            if (stage.getDueDate() != null && stage.getDueDate().isBefore(today)) {
                stage.setStatus(PaymentStageStatus.OVERDUE);
                stageRepository.save(stage);
                count++;
            }
        }
        return count;
    }
}
