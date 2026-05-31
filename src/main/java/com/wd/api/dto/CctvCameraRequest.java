package com.wd.api.dto;

import com.wd.api.model.CctvCamera;
import com.wd.api.model.enums.StreamProtocol;

import java.time.LocalDate;

/**
 * Inbound request body for creating/updating a {@link CctvCamera}.
 *
 * <p>Binding the JPA entity directly as a {@code @RequestBody} (the previous
 * approach) is a mass-assignment risk (SonarQube java:S4684): a client could set
 * server-managed columns such as {@code id}, {@code createdAt} or {@code deletedAt}.
 * This DTO exposes only the client-settable fields and maps to a transient entity;
 * the FK {@code project} is resolved server-side from the path variable.
 */
public record CctvCameraRequest(
        String cameraName,
        String location,
        String provider,
        StreamProtocol streamProtocol,
        String streamUrl,
        String snapshotUrl,
        String username,
        String password,
        Integer port,
        Boolean isActive,
        String resolution,
        LocalDate installationDate,
        Integer displayOrder
) {
    /** Map to a transient entity, preserving the exact (null-passthrough) binding the entity used. */
    public CctvCamera toEntity() {
        CctvCamera c = new CctvCamera();
        c.setCameraName(cameraName);
        c.setLocation(location);
        c.setProvider(provider);
        c.setStreamProtocol(streamProtocol);
        c.setStreamUrl(streamUrl);
        c.setSnapshotUrl(snapshotUrl);
        c.setUsername(username);
        c.setPassword(password);
        c.setPort(port);
        c.setIsActive(isActive);
        c.setResolution(resolution);
        c.setInstallationDate(installationDate);
        c.setDisplayOrder(displayOrder);
        return c;
    }
}
