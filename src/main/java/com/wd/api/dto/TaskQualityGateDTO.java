package com.wd.api.dto;

import com.wd.api.model.TaskQualityGate;

import java.time.LocalDateTime;

public record TaskQualityGateDTO(
        Long id,
        Long taskId,
        String gateType,        // PRELIMINARY | IN_PROGRESS | FINAL
        String status,          // PENDING | PASSED | FAILED | NA
        Long signedByUserId,
        String signedByName,
        LocalDateTime signedAt,
        String notes,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TaskQualityGateDTO from(TaskQualityGate g) {
        var signer = g.getSignedBy();
        String signerName = null;
        if (signer != null) {
            String f = signer.getFirstName();
            String l = signer.getLastName();
            if ((f != null && !f.isBlank()) || (l != null && !l.isBlank())) {
                signerName = ((f == null ? "" : f) + " " + (l == null ? "" : l)).trim();
            }
        }
        return new TaskQualityGateDTO(
                g.getId(),
                g.getTask() != null ? g.getTask().getId() : null,
                g.getGateType() != null ? g.getGateType().name() : null,
                g.getStatus() != null ? g.getStatus().name() : null,
                signer != null ? signer.getId() : null,
                signerName,
                g.getSignedAt(),
                g.getNotes(),
                g.getFailureReason(),
                g.getCreatedAt(),
                g.getUpdatedAt()
        );
    }
}
