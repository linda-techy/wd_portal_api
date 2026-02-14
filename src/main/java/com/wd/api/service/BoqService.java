package com.wd.api.service;

import com.wd.api.dto.*;
import com.wd.api.model.*;
import com.wd.api.repository.*;
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

    public BoqService(BoqItemRepository boqItemRepository,
                      BoqWorkTypeRepository boqWorkTypeRepository,
                      CustomerProjectRepository customerProjectRepository,
                      BoqCategoryRepository categoryRepository,
                      MaterialRepository materialRepository,
                      BoqAuditService auditService) {
        this.boqItemRepository = boqItemRepository;
        this.boqWorkTypeRepository = boqWorkTypeRepository;
        this.customerProjectRepository = customerProjectRepository;
        this.categoryRepository = categoryRepository;
        this.materialRepository = materialRepository;
        this.auditService = auditService;
    }

    // ---- CRUD Operations ----

    public BoqItemResponse createBoqItem(CreateBoqItemRequest request, Long userId) {
        // Validate project exists
        CustomerProject project = customerProjectRepository.findById(request.projectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.projectId()));

        // Validate item_code uniqueness if provided
        if (request.itemCode() != null && !request.itemCode().trim().isEmpty()) {
            validateItemCodeUnique(request.projectId(), request.itemCode(), null);
        }

        BoqItem item = new BoqItem();
        item.setProject(project);
        item.setItemCode(request.itemCode());
        item.setDescription(request.description());
        item.setUnit(request.unit());
        item.setQuantity(request.quantity());
        item.setUnitRate(request.unitRate());
        item.setSpecifications(request.specifications());
        item.setNotes(request.notes());
        item.setStatus("DRAFT");
        item.setIsActive(true);
        item.setExecutedQuantity(BigDecimal.ZERO);
        item.setBilledQuantity(BigDecimal.ZERO);

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
            validateQuantity(request.quantity());
            
            // CRITICAL FIX: Prevent reducing quantity below executed
            if (request.quantity().compareTo(item.getExecutedQuantity()) < 0) {
                throw new IllegalArgumentException(
                    String.format("Cannot reduce planned quantity to %.4f. " +
                        "Already executed: %.4f. Planned quantity must be >= executed quantity.",
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

        if (!item.canApprove()) {
            throw new IllegalStateException("Item cannot be approved. Current status: " + item.getStatus());
        }

        item.setStatus("APPROVED");
        item = boqItemRepository.save(item);

        auditService.logApprove("BOQ_ITEM", id, item.getProject().getId(), userId);

        return BoqItemResponse.fromEntity(item);
    }

    public BoqItemResponse lockBoqItem(Long id, Long userId) {
        BoqItem item = findActiveById(id);

        if (!item.canLock()) {
            throw new IllegalStateException("Only APPROVED items can be locked. Current status: " + item.getStatus());
        }

        item.setStatus("LOCKED");
        item = boqItemRepository.save(item);

        auditService.logLock("BOQ_ITEM", id, item.getProject().getId(), userId);

        return BoqItemResponse.fromEntity(item);
    }

    public BoqItemResponse markAsCompleted(Long id, Long userId) {
        BoqItem item = findActiveById(id);

        // Verify execution is complete
        if (item.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Item cannot be marked completed. Remaining quantity: " + item.getRemainingQuantity());
        }

        item.setStatus("COMPLETED");
        item = boqItemRepository.save(item);

        auditService.logUpdate("BOQ_ITEM", id, item.getProject().getId(), userId, null, Map.of("status", "COMPLETED"));

        return BoqItemResponse.fromEntity(item);
    }

    // ---- Execution & Billing ----

    public BoqItemResponse recordExecution(Long id, RecordExecutionRequest request, Long userId) {
        // CRITICAL FIX: Use synchronized block for simple concurrency protection
        // This prevents race conditions without complex locking mechanisms
        synchronized (this) {
            BoqItem item = findActiveById(id);

            if (!item.canExecute()) {
                throw new IllegalStateException("Execution can only be recorded for APPROVED/LOCKED items. Current status: " + item.getStatus());
            }

            BigDecimal newExecuted = item.getExecutedQuantity().add(request.quantity());

            // Validate: executed <= planned (with clear error message)
            if (newExecuted.compareTo(item.getQuantity()) > 0) {
                throw new IllegalArgumentException(
                        String.format("OVER-EXECUTION PREVENTED: Cannot execute %.4f. " +
                            "Planned: %.4f, Already executed: %.4f, Remaining: %.4f",
                            request.quantity(), item.getQuantity(), item.getExecutedQuantity(),
                            item.getRemainingQuantity()));
            }

            item.setExecutedQuantity(newExecuted);
            item = boqItemRepository.save(item);

            auditService.logExecute("BOQ_ITEM", id, item.getProject().getId(), userId, request.quantity());

            return BoqItemResponse.fromEntity(item);
        }
    }

    public BoqItemResponse recordBilling(Long id, RecordExecutionRequest request, Long userId) {
        // CRITICAL FIX: Simple concurrency protection for billing
        synchronized (this) {
            BoqItem item = findActiveById(id);

            BigDecimal newBilled = item.getBilledQuantity().add(request.quantity());

            // Validate: billed <= executed (with clear error message)
            if (newBilled.compareTo(item.getExecutedQuantity()) > 0) {
                throw new IllegalArgumentException(
                        String.format("OVER-BILLING PREVENTED: Cannot bill %.4f. " +
                            "Executed: %.4f, Already billed: %.4f, Remaining billable: %.4f",
                            request.quantity(), item.getExecutedQuantity(), item.getBilledQuantity(),
                            item.getRemainingBillableQuantity()));
            }

            item.setBilledQuantity(newBilled);
            item = boqItemRepository.save(item);

            auditService.logBill("BOQ_ITEM", id, item.getProject().getId(), userId, request.quantity());

            return BoqItemResponse.fromEntity(item);
        }
    }

    // ---- Queries ----

    @Transactional(readOnly = true)
    public Page<BoqItem> searchBoqItems(BoqSearchFilter filter) {
        Specification<BoqItem> spec = buildSpecification(filter);
        return boqItemRepository.findAll(spec, Objects.requireNonNull(filter.toPageable()));
    }

    @Transactional(readOnly = true)
    public List<BoqItemResponse> getProjectBoq(Long projectId) {
        List<BoqItem> items = boqItemRepository.findByProjectIdAndDeletedAtIsNull(projectId);
        return items.stream()
                .map(BoqItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BoqItemResponse getBoqItemById(Long id) {
        BoqItem item = findActiveById(id);
        return BoqItemResponse.fromEntity(item);
    }

    @Transactional(readOnly = true)
    public BoqFinancialSummary getFinancialSummary(Long projectId) {
        List<BoqItem> items = boqItemRepository.findByProjectIdAndDeletedAtIsNull(projectId);
        CustomerProject project = customerProjectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        int totalItems = items.size();
        int activeItems = (int) items.stream().filter(i -> i.getIsActive()).count();

        BigDecimal totalPlannedCost = items.stream()
                .map(BoqItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExecutedCost = items.stream()
                .map(BoqItem::getTotalExecutedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBilledCost = items.stream()
                .map(BoqItem::getTotalBilledAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCostToComplete = items.stream()
                .map(BoqItem::getCostToComplete)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal overallExecutionPercentage = totalPlannedCost.compareTo(BigDecimal.ZERO) > 0
                ? totalExecutedCost.divide(totalPlannedCost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        BigDecimal overallBillingPercentage = totalExecutedCost.compareTo(BigDecimal.ZERO) > 0
                ? totalBilledCost.divide(totalExecutedCost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        // Category breakdown
        List<BoqFinancialSummary.CategoryFinancialBreakdown> categoryBreakdown = items.stream()
                .filter(i -> i.getCategory() != null)
                .collect(Collectors.groupingBy(BoqItem::getCategory))
                .entrySet().stream()
                .map(e -> {
                    List<BoqItem> catItems = e.getValue();
                    return new BoqFinancialSummary.CategoryFinancialBreakdown(
                            e.getKey().getId(),
                            e.getKey().getName(),
                            catItems.size(),
                            catItems.stream().map(BoqItem::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                            catItems.stream().map(BoqItem::getTotalExecutedAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                            catItems.stream().map(BoqItem::getTotalBilledAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                            catItems.stream().map(BoqItem::getCostToComplete).reduce(BigDecimal.ZERO, BigDecimal::add)
                    );
                })
                .collect(Collectors.toList());

        // Work type breakdown
        List<BoqFinancialSummary.WorkTypeFinancialBreakdown> workTypeBreakdown = items.stream()
                .filter(i -> i.getWorkType() != null)
                .collect(Collectors.groupingBy(BoqItem::getWorkType))
                .entrySet().stream()
                .map(e -> {
                    List<BoqItem> wtItems = e.getValue();
                    return new BoqFinancialSummary.WorkTypeFinancialBreakdown(
                            e.getKey().getId(),
                            e.getKey().getName(),
                            wtItems.size(),
                            wtItems.stream().map(BoqItem::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                            wtItems.stream().map(BoqItem::getTotalExecutedAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                            wtItems.stream().map(BoqItem::getTotalBilledAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                            wtItems.stream().map(BoqItem::getCostToComplete).reduce(BigDecimal.ZERO, BigDecimal::add)
                    );
                })
                .collect(Collectors.toList());

        return new BoqFinancialSummary(
                projectId,
                project.getName(),
                totalItems,
                activeItems,
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

    private void validateQuantity(BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
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
