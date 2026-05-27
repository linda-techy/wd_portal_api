package com.wd.api.dto;

import com.wd.api.model.CctvCamera;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Safe view of a {@link CctvCamera} for API responses.
 *
 * <p>Returning the raw entity is unsafe on two counts: (1) Jackson walks the
 * lazy {@code @ManyToOne CustomerProject project}, serialising the whole
 * project graph (and risking project↔camera recursion under open-in-view), and
 * (2) it exposes the camera's {@code username}/{@code password} RTSP
 * credentials. This DTO carries only the projectId FK and the display fields.
 */
public record CctvCameraResponse(
        Long id,
        Long projectId,
        String cameraName,
        String location,
        String provider,
        String streamProtocol,
        String streamUrl,
        String snapshotUrl,
        Boolean isActive,
        String resolution,
        LocalDate installationDate,
        Integer displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CctvCameraResponse from(CctvCamera c) {
        return new CctvCameraResponse(
                c.getId(),
                c.getProject() != null ? c.getProject().getId() : null,
                c.getCameraName(),
                c.getLocation(),
                c.getProvider(),
                c.getStreamProtocol() != null ? c.getStreamProtocol().name() : null,
                c.getStreamUrl(),
                c.getSnapshotUrl(),
                c.getIsActive(),
                c.getResolution(),
                c.getInstallationDate(),
                c.getDisplayOrder(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
