package com.wd.api.service;

import com.wd.api.dto.*;
import com.wd.api.model.*;
import com.wd.api.model.enums.BoqItemStatus;
import com.wd.api.model.enums.ItemKind;
import com.wd.api.repository.*;
import com.wd.api.security.ProjectAccessGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * BOQ Service - Core business logic for BOQ management.
 * Enforces financial validation rules, status workflow, and audit trail.
 */
@Service
@Transactional
public class BoqService {

    private final BoqItemRepository boqItemRepository;
    private final BoqWorkTypeRepository boqWorkTypeRepository;
    private final CustomerProjectRepository customerProjectRepository;
    private final BoqCategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;
    private final BoqAuditService auditService;
    private final ProjectAccessGuard projectAccessGuard;

    public BoqService(BoqItemRepository boqItemRepository,
                      BoqWorkTypeRepository boqWorkTypeRepository,
                      CustomerProjectRepository customerProjectRepository,
                      BoqCategoryRepository categoryRepository,
                      MaterialRepository materialRepository,
                      BoqAuditService auditService,
                      ProjectAccessGuard projectAccessGuard) {
        this.boqItemRepository = boqItemRepository;
        this.boqWorkTypeRepository = boqWorkTypeRepository;
        this.customerProjectRepository = customerProjectRepository;
        this.categoryRepository = categoryRepository;
        this.materialRepository = materialRepository;
        this.auditService = auditService;
        this.projectAccessGuard = projectAccessGuard;
    }

    // ---- CRUD Operations ----

    public BoqItemResponse createBoqItem(CreateBoqItemRequest request, Long userId) {
        // Validate project exists
        CustomerProject project = customerProjectRepository.findById(request.projectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.projectId()));

        projectAccessGuard.verifyPortalAccess(userId, request.projectId());

        // Validate item_code uniqueness if provided
        if (request.itemCode() != null && !request.itemCode().trim().isEmpty()) {
            validateItemCodeUnique(request.projectId(), request.itemCode(), null);
        }

        ItemKind kind = request.itemKind() != null ? request.itemKind() : ItemKind.BASE;
        validateQuantityForKind(request.quantity(), kind);

        BoqItem item = new BoqItem();
        item.setProject(project);
        item.setItemCode(request.itemCode());
        item.setDescription(request.description());
        item.setUnit(request.unit());
        item.setQuantity(request.quantity());
        item.setUnitRate(request.unitRate());
        item.setSpecifications(request.specifications());
        item.setNotes(request.notes());
        item.setStatus(BoqItemStatus.DRAFT);
        item.setIsActive(true);
        item.setExecutedQuantity(BigDecimal.ZERO);
        item.setBilledQuantity(BigDecimal.ZERO);
        item.setItemKind(kind);

        // Link optional relationships
        if (request.categoryId() != null) {
            BoqCategory category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.categoryId()));
            item.setCategory(category);
        }

        if (request.workTypeId() != null) {
            BoqWorkType workType = boqWorkTypeRepository.findById(request.workTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Work type not found: " + request.workTypeId()));
            item.setWorkType(workType);
        }

        if (request.materialId() != null) {
            Material material = materialRepository.findById(request.materialId())
                    .orElseThrow(() -> new IllegalArgumentException("Material not found: " + request.materialId()));
            item.setMaterial(material);
        }

        item = boqItemRepository.save(item);

        auditService.logCreate("BOQ_ITEM", item.getId(), project.getId(), userId, item);

        return BoqItemResponse.fromEntity(item);
    }

    public BoqItemResponse updateBoqItem(Long id, UpdateBoqItemRequest request, Long userId) {
        BoqItem item = findActiveById(id);

        projectAccessGuard.verifyPortalAccess(userId, item.getProject().getId());

        // Enforce status workflow: only DRAFT items can be edited
        if (!item.isDraft()) {
            throw new IllegalStateException("Only DRAFT items can be edited. Current status: " + item.getStatus());
        }

        // Validate item_code uniqueness if changed
        if (request.itemCode() != null && !request.itemCode().equals(item.getItemCode())) {
            validateItemCodeUnique(item.getProject().getId(), request.itemCode(), id);
        }

        // Capture old state for audit
        Map<String, Object> oldState = captureItemState(item);

        // Update fields
        if (request.itemCode() != null) item.setItemCode(request.itemCode());
        if (request.description() != null) item.setDescription(request.description());
        if (request.unit() != null) item.setUnit(request.unit());
        if (request.quantity() != null) {
            ItemKind resolvedKind = request.itemKind() != null ? request.itemKind() : item.getItemKind();
            validateQuantityForKind(request.quantity(), resolvedKind);

            // CRITICAL FIX: Prevent reducing quantity below executed
            if (request.quantity().compareTo(item.getExecutedQuantity()) < 0) {
                throw new IllegalArgumentException(
                    String.format("Cannot reduce planned quantity to %.6f. " +
                        "Already executed: %.6f. Planned quantity must be >= executed quantity.",
                        request.quantity(), item.getExecutedQuantity())
                );
            }
            
            item.setQuantity(request.quantity());
        }
        if (request.unitRate() != null) {
            validateUnitRate(request.unitRate());
            item.setUnitRate(request.unitRate());
        }
        if (request.specifications() != null) item.setSpecifications(request.specifications());
        if (request.notes() != null) item.setNotes(request.notes());
        if (request.quantity() == null && request.itemKind() != null && request.itemKind() != item.getItemKind()) {
            // Kind is changing without a new quantity — validate existing quantity against new kind
            validateQuantityForKind(item.getQuantity(), request.itemKind());
        }
        if (request.itemKind() != null) item.setItemKind(request.itemKind());

        // Update optional relationships
        if (request.categoryId() != null) {
            BoqCategory category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            item.setCategory(category);
        }

        if (request.workTypeId() != null) {
            BoqWorkType workType = boqWorkTypeRepository.findById(request.workTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Work type not found"));
            item.setWorkType(workType);
        }

        if (request.materialId() != null) {
            Material material = materialRepository.findById(request.materialId())
                    .orElseThrow(() -> new IllegalArgumentException("Material not found"));
            item.setMaterial(material);
        }

        item = boqItemRepository.save(item);

        Map<String, Object> newState = captureItemState(item);
        auditService.logUpdate("BOQ_ITEM", id, item.getProject().getId(), userId, oldState, newState);

        return BoqItemResponse.fromEntity(item);
    }

    public void softDeleteBoqItem(Long id, Long userId) {
        BoqItem item = findActiveById(id);

        projectAccessGuard.verifyPortalAccess(userId, item.getProject().getId());

        // Enforce: only DRAFT items can be deleted
        if (!item.isDraft()) {
            throw new IllegalStateException("Only DRAFT items can be deleted. Current status: " + item.getStatus());
        }

        item.setDeletedAt(LocalDateTime.now());
        item.setDeletedByUserId(userId);
        item.setIsActive(false);

        boqItemRepository.save(item);

        auditService.logDelete("BOQ_ITEM", id, item.getProject().getId(), userId);
    }

    // ---- Status Workflow ----

    public BoqItemResponse approveBoqItem(Long id, Long userId) {
        BoqItem item = findActiveById(id);

        projectAccessGuard.verifyPortalAccess(userId, item.getProject().getId());

        if (!item.canApprove()) {
            throw new IllegalStateException("Item cannot be approved. Current status: " + item.getStatus());
        }

        item.setStatus(BoqItemStatus.APPROVED);
        item = boqItemRepository.save(item);

        auditService.logApprove("BOQ_ITEM", id, item.getProject().getId(), userId);

        return BoqItemResponse.fromEntity(item);
    }

    public BoqItemResponse lockBoqItem(Long id, Long userId) {
        BoqItem item = findActiveById(id);

        projectAccessGuard.verifyPortalAccess(userId, item.getProject().getId());

        if (!item.canLock()) {
            throw new IllegalStateException("Only APPROVED items can be locked. Current status: " + item.getStatus());
        }

        item.setStatus(BoqItemStatus.LOCKED);
        item = boqItemRepository.save(item);

        auditService.logLock("BOQ_ITEM", id, item.getProject().getId(), userId);

        return BoqItemResponse.fromEntity(item);
    }

    public BoqItemResponse markAsCompleted(Long id, Long userId) {
        BoqItem item = findActiveById(id);

        projectAccessGuard.verifyPortalAccess(userId, item.getProject().getId());

        // Only LOCKED items can be completed (must progress through full workflow)
        if (!item.isLocked()) {
            throw new IllegalStateException(
                "Only LOCKED items can be marked completed. Current status: " + item.getStatus() +
                ". Items must be APPROVED then LOCKED before completing.");
        }

        // Verify execution is complete
        if (item.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Item cannot be marked completed. Remaining quantity: " + item.getRemainingQuantity());
        }

        item.setStatus(BoqItemStatus.COMPLETED);
        item = boqItemRepository.save(item);

        auditService.logUpdate("BOQ_ITEM", id, item.getProject().getId(), userId, null, Map.of("status", "COMPLETED"));

        return BoqItemResponse.fromEntity(item);
    }

    // ---- Execution & Billing ----

    public BoqItemResponse recordExecution(Long id, RecordExecutionRequest request, Long userId) {
        BoqItem item = boqItemRepository.findByIdWithLock(id)
                .orElseThrow(() -> new IllegalArgumentException("BOQ item not found: " + id));

        projectAccessGuard.verifyPortalAccess(userId, item.getProject().getId());

        if (!item.canExecute()) {
            throw new IllegalStateException("Execution can only be recorded for APPROVED/LOCKED items. Current status: " + item.getStatus());
        }

        BigDecimal newExecuted = item.getExecutedQuantity().add(request.quantity());

        // Validate: executed <= planned (with clear error message)
        if (newExecuted.compareTo(item.getQuantity()) > 0) {
            throw new IllegalArgumentException(
                    String.format("OVER-EXECUTION PREVENTED: Cannot execute %.6f. " +
                        "Planned: %.6f, Already executed: %.6f, Remaining: %.6f",
                        request.quantity(), item.getQuantity(), item.getExecutedQuantity(),
                        item.getRemainingQuantity()));
        }

        item.setExecutedQuantity(newExecuted);
        item = boqItemRepository.saveAndFlush(item);

        auditService.logExecute("BOQ_ITEM", id, item.getProject().getId(), userId, request.quantity());

        return BoqItemResponse.fromEntity(item);
    }

    public BoqItemResponse recordBilling(Long id, RecordExecutionRequest request, Long userId) {
        BoqItem item = boqItemRepository.findByIdWithLock(id)
                .orElseThrow(() -> new IllegalArgumentException("BOQ item not found: " + id));

        projectAccessGuard.verifyPortalAccess(userId, item.getProject().getId());

        if (!item.canExecute()) {
            throw new IllegalStateException("Item must be APPROVED or LOCKED to record billing. Current status: " + item.getStatus());
        }

        BigDecimal newBilled = item.getBilledQuantity().add(request.quantity());

        // Validate: billed <= executed (with clear error message)
        if (newBilled.compareTo(item.getExecutedQuantity()) > 0) {
            throw new IllegalArgumentException(
                    String.format("OVER-BILLING PREVENTED: Cannot bill %.6f. " +
                        "Executed: %.6f, Already billed: %.6f, Remaining billable: %.6f",
                        request.quantity(), item.getExecutedQuantity(), item.getBilledQuantity(),
                        item.getRemainingBillableQuantity()));
        }

        item.setBilledQuantity(newBilled);
        item = boqItemRepository.saveAndFlush(item);

        auditService.logBill("BOQ_ITEM", id, item.getProject().getId(), userId, request.quantity());

        return BoqItemResponse.fromEntity(item);
    }

    public BoqItemResponse correctExecution(Long id, CorrectionRequest request, Long userId) {
        BoqItem item = boqItemRepository.findByIdWithLock(id)
                .orElseThrow(() -> new IllegalArgumentException("BOQ item not found: " + id));

        projectAccessGuard.verifyPortalAccess(userId, item.getProject().getId());

        BigDecimal currentValue;
        BigDecimal newValue;

        if (request.type() == CorrectionRequest.CorrectionType.REDUCE_EXECUTION) {
            currentValue = item.getExecutedQuantity();
            newValue = currentValue.subtract(request.amount());

            if (newValue.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Executed quantity cannot be reduced below zero");
            }
            if (newValue.compareTo(item.getBilledQuantity()) < 0) {
                throw new IllegalArgumentException(
                    String.format("Cannot reduce executed quantity to %.6f. Already billed: %.6f.",
                        newValue, item.getBilledQuantity())
                );
            }

            item.setExecutedQuantity(newValue);

        } else { // REDUCE_BILLING
            currentValue = item.getBilledQuantity();
            newValue = currentValue.subtract(request.amount());

            if (newValue.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Billed quantity cannot be reduced below zero");
            }

            item.setBilledQuantity(newValue);
        }

        item = boqItemRepository.saveAndFlush(item);

        Map<String, Object> correctionDetails = new HashMap<>();
        correctionDetails.put("type", request.type().name());
        correctionDetails.put("oldValue", currentValue);
        correctionDetails.put("newValue", newValue);
        correctionDetails.put("correctionAmount", request.amount());
        correctionDetails.put("reason", request.reason());
        correctionDetails.put("reference", request.referenceNumber() != null ? request.referenceNumber() : "N/A");

        auditService.logUpdate("BOQ_ITEM", id, item.getProject().getId(), userId,
            Map.of(request.type().name(), currentValue),
            correctionDetails
        );

        return BoqItemResponse.fromEntity(item);
    }

    // ---- Queries ----

    @Transactional(readOnly = true)
    public Page<BoqItemResponse> searchBoqItems(BoqSearchFilter filter) {
        Specification<BoqItem> spec = buildSpecification(filter);
        return boqItemRepository.findAll(spec, Objects.requireNonNull(filter.toPageable()))
                .map(BoqItemResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<BoqItemResponse> getProjectBoq(Long projectId, Long userId) {
        projectAccessGuard.verifyPortalAccess(userId, projectId);
        List<BoqItem> items = boqItemRepository.findByProjectIdWithAssociations(projectId);
        return items.stream()
                .map(BoqItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Paginated project BOQ — used by Portal UI (replaces full-list load).
     * Filters are optional; page/size default to 0/50 if not supplied.
     */
    @Transactional(readOnly = true)
    public Page<BoqItemResponse> getProjectBoqPaged(Long userId, Long projectId, int page, int size,
                                                     Long workTypeId, Long categoryId, String status) {
        projectAccessGuard.verifyPortalAccess(userId, projectId);
        BoqSearchFilter filter = new BoqSearchFilter();
        filter.setProjectId(projectId);
        filter.setWorkTypeId(workTypeId);
        filter.setCategoryId(categoryId);
        filter.setStatus(status);
        filter.setPage(page);
        filter.setSize(Math.min(Math.max(size, 1), 200));
        filter.setSortBy("id");
        filter.setSortDirection("asc");
        Specification<BoqItem> spec = buildSpecification(filter);
        return boqItemRepository.findAll(spec, Objects.requireNonNull(filter.toPageable()))
                .map(BoqItemResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public BoqItemResponse getBoqItemById(Long id, Long userId) {
        BoqItem item = findActiveById(id);
        projectAccessGuard.verifyPortalAccess(userId, item.getProject().getId());
        return BoqItemResponse.fromEntity(item);
    }

    @Transactional(readOnly = true)
    public BoqFinancialSummary getFinancialSummary(Long userId, Long projectId) {
        projectAccessGuard.verifyPortalAccess(userId, projectId);
        CustomerProject project = customerProjectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        // Use SQL aggregation — avoids loading all BOQ entities into memory
        List<Object[]> totalRows = boqItemRepository.getFinancialTotals(projectId);
        Object[] totals = (totalRows != null && !totalRows.isEmpty()) ? totalRows.get(0) : new Object[6];
        long totalItems = totals[0] != null ? ((Number) totals[0]).longValue() : 0L;
        long activeItems = totals[1] != null ? ((Number) totals[1]).longValue() : 0L;
        BigDecimal totalPlannedCost  = toBD(totals[2]);
        BigDecimal totalExecutedCost = toBD(totals[3]);
        BigDecimal totalBilledCost   = toBD(totals[4]);
        BigDecimal totalCostToComplete = toBD(totals[5]);

        BigDecimal overallExecutionPercentage = totalPlannedCost.compareTo(BigDecimal.ZERO) > 0
                ? totalExecutedCost.divide(totalPlannedCost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        BigDecimal overallBillingPercentage = totalExecutedCost.compareTo(BigDecimal.ZERO) > 0
                ? totalBilledCost.divide(totalExecutedCost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        List<BoqFinancialSummary.CategoryFinancialBreakdown> categoryBreakdown =
                boqItemRepository.getFinancialCategoryBreakdown(projectId).stream()
                        .map(r -> new BoqFinancialSummary.CategoryFinancialBreakdown(
                                ((Number) r[0]).longValue(),
                                (String) r[1],
                                ((Number) r[2]).intValue(),
                                toBD(r[3]), toBD(r[4]), toBD(r[5]), toBD(r[6])
                        ))
                        .collect(Collectors.toList());

        List<BoqFinancialSummary.WorkTypeFinancialBreakdown> workTypeBreakdown =
                boqItemRepository.getFinancialWorkTypeBreakdown(projectId).stream()
                        .map(r -> new BoqFinancialSummary.WorkTypeFinancialBreakdown(
                                ((Number) r[0]).longValue(),
                                (String) r[1],
                                ((Number) r[2]).intValue(),
                                toBD(r[3]), toBD(r[4]), toBD(r[5]), toBD(r[6])
                        ))
                        .collect(Collectors.toList());

        return new BoqFinancialSummary(
                projectId,
                project.getName(),
                (int) totalItems,
                (int) activeItems,
                totalPlannedCost,
                totalExecutedCost,
                totalBilledCost,
                totalCostToComplete,
                overallExecutionPercentage,
                overallBillingPercentage,
                categoryBreakdown,
                workTypeBreakdown
        );
    }

    private static BigDecimal toBD(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    @Transactional(readOnly = true)
    public List<BoqAuditLog> getAuditLog(Long itemId) {
        return auditService.getAuditLogForItem(itemId);
    }

    @Transactional(readOnly = true)
    public List<BoqWorkType> getAllWorkTypes() {
        return boqWorkTypeRepository.findAll(Sort.by("displayOrder", "name"));
    }

    // ---- Private Helpers ----

    private BoqItem findActiveById(Long id) {
        BoqItem item = boqItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("BOQ item not found: " + id));
        
        if (item.isDeleted()) {
            throw new IllegalArgumentException("BOQ item has been deleted: " + id);
        }
        
        return item;
    }

    private void validateItemCodeUnique(Long projectId, String itemCode, Long excludeItemId) {
        if (itemCode == null || itemCode.trim().isEmpty()) return;

        List<BoqItem> existing = boqItemRepository.findByProjectIdAndItemCodeAndDeletedAtIsNull(projectId, itemCode);
        existing = existing.stream()
                .filter(i -> !i.getId().equals(excludeItemId))
                .collect(Collectors.toList());

        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("Item code '" + itemCode + "' already exists in this project");
        }
    }

    private void validateQuantityForKind(BigDecimal quantity, ItemKind kind) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if ((ItemKind.BASE == kind || ItemKind.ADDON == kind)
                && quantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException(
                kind.name() + " items must have quantity > 0. " +
                "Use OPTIONAL or EXCLUSION for zero-quantity scope items.");
        }
    }

    private void validateUnitRate(BigDecimal unitRate) {
        if (unitRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit rate cannot be negative");
        }
    }

    private Map<String, Object> captureItemState(BoqItem item) {
        Map<String, Object> state = new HashMap<>();
        state.put("id", item.getId());
        state.put("itemCode", item.getItemCode());
        state.put("description", item.getDescription());
        state.put("quantity", item.getQuantity());
        state.put("unitRate", item.getUnitRate());
        state.put("status", item.getStatus());
        return state;
    }

    private Specification<BoqItem> buildSpecification(BoqSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude soft-deleted items
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), searchPattern),
                        cb.like(cb.lower(root.get("notes")), searchPattern),
                        cb.like(cb.lower(root.get("itemCode")), searchPattern)));
            }

            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            }

            if (filter.getWorkTypeId() != null) {
                predicates.add(cb.equal(root.get("workType").get("id"), filter.getWorkTypeId()));
            }

            if (filter.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
            }

            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                try {
                    predicates.add(cb.equal(root.get("status"),
                            com.wd.api.model.enums.BoqItemStatus.valueOf(filter.getStatus().toUpperCase())));
                } catch (IllegalArgumentException ignored) {
                    // Invalid status value — skip filter rather than crash
                }
            }

            if (filter.getItemCode() != null && !filter.getItemCode().isEmpty()) {
                predicates.add(
                        cb.like(cb.lower(root.get("itemCode")),
                                "%" + filter.getItemCode().toLowerCase() + "%"));
            }

            if (filter.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), filter.getMinAmount()));
            }
            if (filter.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), filter.getMaxAmount()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
