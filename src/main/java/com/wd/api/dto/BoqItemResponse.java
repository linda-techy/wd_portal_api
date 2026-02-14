package com.wd.api.dto;

import com.wd.api.model.BoqItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BoqItemResponse(
        Long id,
        Long projectId,
        String projectName,
        Long categoryId,
        String categoryName,
        Long workTypeId,
        String workTypeName,
        Long materialId,
        String materialName,
        String itemCode,
        String description,
        String unit,
        BigDecimal quantity,
        BigDecimal unitRate,
        BigDecimal totalAmount,
        BigDecimal executedQuantity,
        BigDecimal billedQuantity,
        BigDecimal remainingQuantity,
        BigDecimal remainingBillableQuantity,
        BigDecimal totalExecutedAmount,
        BigDecimal totalBilledAmount,
        BigDecimal costToComplete,
        BigDecimal executionPercentage,
        BigDecimal billingPercentage,
        String status,
        Boolean isActive,
        String specifications,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long createdByUserId,
        String createdByName,
        Long updatedByUserId,
        String updatedByName
) {

    public static BoqItemResponse fromEntity(BoqItem item) {
        String categoryName = item.getCategory() != null ? item.getCategory().getName() : null;
        String workTypeName = item.getWorkType() != null ? item.getWorkType().getName() : null;
        String materialName = item.getMaterial() != null ? item.getMaterial().getName() : null;
        String projectName = item.getProject() != null ? item.getProject().getName() : null;

        String createdByName = null;
        String updatedByName = null;

        // Note: BaseEntity uses @CreatedBy/@LastModifiedBy which should populate user IDs
        // For now, we'll leave names null as the BaseEntity doesn't track PortalUser references

        return new BoqItemResponse(
                item.getId(),
                item.getProject() != null ? item.getProject().getId() : null,
                projectName,
                item.getCategory() != null ? item.getCategory().getId() : null,
                categoryName,
                item.getWorkType() != null ? item.getWorkType().getId() : null,
                workTypeName,
                item.getMaterial() != null ? item.getMaterial().getId() : null,
                materialName,
                item.getItemCode(),
                item.getDescription(),
                item.getUnit(),
                item.getQuantity(),
                item.getUnitRate(),
                item.getTotalAmount(),
                item.getExecutedQuantity(),
                item.getBilledQuantity(),
                item.getRemainingQuantity(),
                item.getRemainingBillableQuantity(),
                item.getTotalExecutedAmount(),
                item.getTotalBilledAmount(),
                item.getCostToComplete(),
                item.getExecutionPercentage(),
                item.getBillingPercentage(),
                item.getStatus(),
                item.getIsActive(),
                item.getSpecifications(),
                item.getNotes(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getCreatedByUserId(),
                createdByName,
                item.getUpdatedByUserId(),
                updatedByName
        );
    }
}
