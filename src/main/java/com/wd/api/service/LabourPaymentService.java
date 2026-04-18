package com.wd.api.service;

import com.wd.api.dto.LabourPaymentDTO;
import com.wd.api.model.*;
import com.wd.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Handles labour wage processing and wage sheet validation (split from FinanceService god class).
 */
@Service
@RequiredArgsConstructor
public class LabourPaymentService {

    private final LabourPaymentRepository labourPaymentRepository;
    private final LabourRepository labourRepository;
    private final CustomerProjectRepository projectRepository;
    private final MeasurementBookRepository mbRepository;
    private final WageSheetRepository wageSheetRepository;

    @Transactional
    public LabourPaymentDTO recordLabourPayment(LabourPaymentDTO dto) {
        Long labourId = java.util.Objects.requireNonNull(dto.getLabourId());
        Labour labour = labourRepository.findById(labourId)
                .orElseThrow(() -> new RuntimeException("Labour not found"));
        Long projectId = java.util.Objects.requireNonNull(dto.getProjectId());
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        MeasurementBook mbEntry = null;
        if (dto.getMbEntryId() != null) {
            Long mbId = java.util.Objects.requireNonNull(dto.getMbEntryId());
            mbEntry = mbRepository.findById(mbId).orElse(null);
        }

        // Validate against wage sheet if provided
        WageSheet wageSheet = null;
        if (dto.getWageSheetId() != null) {
            wageSheet = wageSheetRepository.findById(dto.getWageSheetId())
                    .orElseThrow(() -> new RuntimeException("WageSheet not found"));
            if (wageSheet.getStatus() != WageSheet.SheetStatus.APPROVED) {
                throw new IllegalStateException("Can only record payments against APPROVED wage sheets");
            }
            BigDecimal paidSoFar = labourPaymentRepository.sumPaymentsByWageSheetAndLabour(
                    dto.getWageSheetId(), dto.getLabourId());
            WageSheetEntry entry = wageSheet.getEntries().stream()
                    .filter(e -> e.getLabour().getId().equals(dto.getLabourId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Labour not found in this wage sheet"));
            BigDecimal remaining = entry.getNetPayable().subtract(paidSoFar);
            if (dto.getAmount().compareTo(remaining) > 0) {
                throw new IllegalArgumentException(
                        "Payment amount " + dto.getAmount() + " exceeds remaining balance " + remaining);
            }
        }

        LabourPayment payment = LabourPayment.builder()
                .labour(labour)
                .project(project)
                .mbEntry(mbEntry)
                .wageSheet(wageSheet)
                .amount(dto.getAmount())
                .paymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : java.time.LocalDate.now())
                .paymentMethod(dto.getPaymentMethod())
                .notes(dto.getNotes())
                .build();

        return mapToDTO(labourPaymentRepository.save(payment));
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private LabourPaymentDTO mapToDTO(LabourPayment p) {
        return LabourPaymentDTO.builder()
                .id(p.getId())
                .labourId(p.getLabour().getId())
                .labourName(p.getLabour().getName())
                .projectId(p.getProject().getId())
                .projectName(p.getProject().getName())
                .mbEntryId(p.getMbEntry() != null ? p.getMbEntry().getId() : null)
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate())
                .paymentMethod(p.getPaymentMethod())
                .notes(p.getNotes())
                .build();
    }
}
