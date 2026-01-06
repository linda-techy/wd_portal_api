package com.wd.api.dto;

import java.time.LocalDateTime;

/**
 * DTOs for project modules (Documents, etc.)
 */
public class ProjectModuleDtos {

    // ===== DOCUMENT MODULE DTOs =====

    public record DocumentCategoryDto(
            Long id,
            String name,
            String description,
            Integer displayOrder) {
    }

    public record ProjectDocumentDto(
            Long id,
            Long projectId,
            Long categoryId,
            String categoryName,
            String filename,
            String filePath,
            String downloadUrl,
            Long fileSize,
            String fileType,
            Long uploadedById,
            String uploadedByName,
            LocalDateTime uploadDate,
            String description,
            Integer version,
            Boolean isActive) {
    }

    public record DocumentUploadRequest(
            Long categoryId,
            String description) {
    }

}
