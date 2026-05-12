package com.wd.api.service;

import com.wd.api.dto.*;
import com.wd.api.model.*;
import com.wd.api.model.enums.BoqItemStatus;
import com.wd.api.model.enums.ItemKind;
import com.wd.api.repository.*;
import com.wd.api.security.ProjectAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BoqService business logic.
 * Uses Mockito mocks for all repository/guard dependencies — no Spring context.
 */
@ExtendWith(MockitoExtension.class)
class BoqServiceTest {

    @Mock private BoqItemRepository boqItemRepository;
    @Mock private BoqWorkTypeRepository boqWorkTypeRepository;
    @Mock private CustomerProjectRepository customerProjectRepository;
    @Mock private BoqCategoryRepository categoryRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private BoqAuditService auditService;
    @Mock private ProjectAccessGuard projectAccessGuard;

    @InjectMocks
    private BoqService boqService;

    private CustomerProject project;

    @BeforeEach
    void setUp() {
        project = new CustomerProject();
        project.setId(1L);
        project.setName("Test Project");
    }

    // ── Helper builders ───────────────────────────────────────────────────────

    private BoqItem approvedItem(BigDecimal quantity) {
        BoqItem item = new BoqItem();
        item.setId(10L);
        item.setProject(project);
        item.setDescription("Concrete work");
        item.setQuantity(quantity);
        item.setUnitRate(new BigDecimal("500.00"));
        item.setExecutedQuantity(BigDecimal.ZERO);
        item.setBilledQuantity(BigDecimal.ZERO);
        item.setStatus(BoqItemStatus.APPROVED);
        item.setIsActive(true);
        item.setItemKind(ItemKind.BASE);
        return item;
    }

    private BoqItem draftItem() {
        BoqItem item = new BoqItem();
        item.setId(10L);
        item.setProject(project);
        item.setDescription("Draft item");
        item.setQuantity(new BigDecimal("10.00"));
        item.setUnitRate(new BigDecimal("100.00"));
        item.setExecutedQuantity(BigDecimal.ZERO);
        item.setBilledQuantity(BigDecimal.ZERO);
        item.setStatus(BoqItemStatus.DRAFT);
        item.setIsActive(true);
        item.setItemKind(ItemKind.BASE);
        return item;
    }

    private CreateBoqItemRequest createRequest(Long projectId, String desc, BigDecimal qty, BigDecimal rate) {
        // "995411" — SAC for general construction services of buildings.
        return new CreateBoqItemRequest(projectId, null, null, null, "995411", desc, "SqFt", qty, rate,
                null, null, null, ItemKind.BASE);
    }

    // ── createBoqItem ─────────────────────────────────────────────────────────

    @Test
    void createBoqItem_validRequest_returnsItemResponse() {
        // arrange
        when(customerProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());

        BoqItem saved = draftItem();
        when(boqItemRepository.save(any(BoqItem.class))).thenReturn(saved);

        CreateBoqItemRequest req = createRequest(1L, "Plastering", new BigDecimal("50.00"), new BigDecimal("200.00"));

        // act
        BoqItemResponse response = boqService.createBoqItem(req, 99L);

        // assert
        assertThat(response).isNotNull();
        verify(boqItemRepository).save(any(BoqItem.class));
        verify(auditService).logCreate(eq("BOQ_ITEM"), anyLong(), eq(1L), eq(99L), any());
    }

    @Test
    void createBoqItem_projectNotFound_throwsIllegalArgumentException() {
        when(customerProjectRepository.findById(999L)).thenReturn(Optional.empty());
        CreateBoqItemRequest req = createRequest(999L, "Work", new BigDecimal("10.00"), new BigDecimal("100.00"));

        assertThatThrownBy(() -> boqService.createBoqItem(req, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void createBoqItem_zeroQuantityForBaseKind_throwsIllegalArgumentException() {
        when(customerProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());

        CreateBoqItemRequest req = new CreateBoqItemRequest(1L, null, null, null, "995411",
                "Concrete", "SqFt", BigDecimal.ZERO, new BigDecimal("100.00"),
                null, null, null, ItemKind.BASE);

        assertThatThrownBy(() -> boqService.createBoqItem(req, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must have quantity > 0");
    }

    @Test
    void createBoqItem_negativeQuantity_throwsIllegalArgumentException() {
        when(customerProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());

        CreateBoqItemRequest req = new CreateBoqItemRequest(1L, null, null, null, "995411",
                "Concrete", "SqFt", new BigDecimal("-5.00"), new BigDecimal("100.00"),
                null, null, null, ItemKind.BASE);

        assertThatThrownBy(() -> boqService.createBoqItem(req, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    /** G-61: When the BOQ author leaves HSN blank but picks a material that
     *  has its own HSN/SAC, the line inherits it from the material master. */
    @Test
    void createBoqItem_blankHsnInheritsFromMaterial() {
        when(customerProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());

        Material material = new Material();
        material.setId(77L);
        material.setName("OPC 53-grade cement");
        material.setHsnSacCode("2523");
        when(materialRepository.findById(77L)).thenReturn(Optional.of(material));

        ArgumentCaptor<BoqItem> captor = ArgumentCaptor.forClass(BoqItem.class);
        BoqItem saved = draftItem();
        when(boqItemRepository.save(captor.capture())).thenReturn(saved);

        CreateBoqItemRequest req = new CreateBoqItemRequest(1L, null, null, null,
                /* hsnSacCode blank */ null,
                "Cement supply", "Bag", new BigDecimal("100.00"), new BigDecimal("420.00"),
                /* materialId */ 77L, null, null, ItemKind.BASE);

        boqService.createBoqItem(req, 1L);

        BoqItem persisted = captor.getValue();
        assertThat(persisted.getHsnSacCode()).isEqualTo("2523");
        assertThat(persisted.getMaterial()).isEqualTo(material);
    }

    /** G-21/G-61: when neither the line nor the material has an HSN, creation
     *  must fail with a clear error — invoices cannot be generated without it. */
    @Test
    void createBoqItem_blankHsnAndMaterialWithoutHsn_throws() {
        when(customerProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());

        Material material = new Material();
        material.setId(78L);
        material.setName("Misc material");
        // hsn_sac_code intentionally null
        when(materialRepository.findById(78L)).thenReturn(Optional.of(material));

        CreateBoqItemRequest req = new CreateBoqItemRequest(1L, null, null, null,
                null, "Misc work", "Each", new BigDecimal("5.00"), new BigDecimal("100.00"),
                78L, null, null, ItemKind.BASE);

        assertThatThrownBy(() -> boqService.createBoqItem(req, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HSN/SAC code is required");
    }

    @Test
    void createBoqItem_setsStatusToDraftAndExecutedToZero() {
        when(customerProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());

        ArgumentCaptor<BoqItem> captor = ArgumentCaptor.forClass(BoqItem.class);
        BoqItem saved = draftItem();
        when(boqItemRepository.save(captor.capture())).thenReturn(saved);

        CreateBoqItemRequest req = createRequest(1L, "Tiling", new BigDecimal("20.00"), new BigDecimal("300.00"));
        boqService.createBoqItem(req, 1L);

        BoqItem persisted = captor.getValue();
        assertThat(persisted.getStatus()).isEqualTo(BoqItemStatus.DRAFT);
        assertThat(persisted.getExecutedQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(persisted.getBilledQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /** G-21: HSN/SAC code from the request must land on the entity for GST invoicing. */
    @Test
    void createBoqItem_persistsHsnSacCodeFromRequest() {
        when(customerProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());

        ArgumentCaptor<BoqItem> captor = ArgumentCaptor.forClass(BoqItem.class);
        when(boqItemRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        CreateBoqItemRequest req = createRequest(1L, "RCC Slab",
                new BigDecimal("100.00"), new BigDecimal("450.00"));
        boqService.createBoqItem(req, 1L);

        assertThat(captor.getValue().getHsnSacCode()).isEqualTo("995411");
    }

    // ── recordExecution ───────────────────────────────────────────────────────

    @Test
    void recordExecution_quantityWithinLimit_updatesExecutedQuantity() {
        BoqItem item = approvedItem(new BigDecimal("100.00"));
        when(boqItemRepository.findByIdWithLock(10L)).thenReturn(Optional.of(item));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());
        when(boqItemRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        RecordExecutionRequest req = new RecordExecutionRequest(new BigDecimal("40.00"), null, null);
        BoqItemResponse resp = boqService.recordExecution(10L, req, 1L);

        assertThat(resp).isNotNull();
        assertThat(item.getExecutedQuantity()).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    void recordExecution_quantityExceedsPlanned_throwsIllegalArgumentException() {
        BoqItem item = approvedItem(new BigDecimal("100.00"));
        item.setExecutedQuantity(new BigDecimal("90.00"));
        when(boqItemRepository.findByIdWithLock(10L)).thenReturn(Optional.of(item));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());

        RecordExecutionRequest req = new RecordExecutionRequest(new BigDecimal("20.00"), null, null);

        assertThatThrownBy(() -> boqService.recordExecution(10L, req, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OVER-EXECUTION PREVENTED");
    }

    @Test
    void recordExecution_draftItem_throwsIllegalStateException() {
        BoqItem item = draftItem();
        when(boqItemRepository.findByIdWithLock(10L)).thenReturn(Optional.of(item));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());

        RecordExecutionRequest req = new RecordExecutionRequest(new BigDecimal("5.00"), null, null);

        assertThatThrownBy(() -> boqService.recordExecution(10L, req, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED/LOCKED");
    }

    // ── recordBilling ─────────────────────────────────────────────────────────

    @Test
    void recordBilling_billedQuantityWithinExecuted_updatesBilledQuantity() {
        BoqItem item = approvedItem(new BigDecimal("100.00"));
        item.setExecutedQuantity(new BigDecimal("60.00"));
        when(boqItemRepository.findByIdWithLock(10L)).thenReturn(Optional.of(item));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());
        when(boqItemRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        RecordExecutionRequest req = new RecordExecutionRequest(new BigDecimal("30.00"), null, null);
        boqService.recordBilling(10L, req, 1L);

        assertThat(item.getBilledQuantity()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void recordBilling_billedExceedsExecuted_throwsIllegalArgumentException() {
        BoqItem item = approvedItem(new BigDecimal("100.00"));
        item.setExecutedQuantity(new BigDecimal("20.00"));
        when(boqItemRepository.findByIdWithLock(10L)).thenReturn(Optional.of(item));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());

        RecordExecutionRequest req = new RecordExecutionRequest(new BigDecimal("50.00"), null, null);

        assertThatThrownBy(() -> boqService.recordBilling(10L, req, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OVER-BILLING PREVENTED");
    }

    // ── correctExecution ──────────────────────────────────────────────────────

    @Test
    void correctExecution_reduceExecution_reducesQuantityCorrectly() {
        BoqItem item = approvedItem(new BigDecimal("100.00"));
        item.setExecutedQuantity(new BigDecimal("50.00"));
        when(boqItemRepository.findByIdWithLock(10L)).thenReturn(Optional.of(item));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());
        when(boqItemRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        CorrectionRequest req = new CorrectionRequest(
                CorrectionRequest.CorrectionType.REDUCE_EXECUTION,
                new BigDecimal("10.00"),
                "Client requested revision",
                "REF-001");

        boqService.correctExecution(10L, req, 1L);

        assertThat(item.getExecutedQuantity()).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    void correctExecution_cannotReduceBelowBilledQuantity_throwsIllegalArgumentException() {
        BoqItem item = approvedItem(new BigDecimal("100.00"));
        item.setExecutedQuantity(new BigDecimal("50.00"));
        item.setBilledQuantity(new BigDecimal("45.00"));
        when(boqItemRepository.findByIdWithLock(10L)).thenReturn(Optional.of(item));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());

        CorrectionRequest req = new CorrectionRequest(
                CorrectionRequest.CorrectionType.REDUCE_EXECUTION,
                new BigDecimal("20.00"),
                "Correction requested by PM",
                null);

        assertThatThrownBy(() -> boqService.correctExecution(10L, req, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Already billed");
    }

    // ── softDeleteBoqItem ─────────────────────────────────────────────────────

    @Test
    void softDeleteBoqItem_draftItem_setsDeletedAtAndIsActive() {
        BoqItem item = draftItem();
        when(boqItemRepository.findById(10L)).thenReturn(Optional.of(item));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());
        when(boqItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boqService.softDeleteBoqItem(10L, 1L);

        assertThat(item.getDeletedAt()).isNotNull();
        assertThat(item.getIsActive()).isFalse();
        assertThat(item.getDeletedByUserId()).isEqualTo(1L);
        verify(auditService).logDelete("BOQ_ITEM", 10L, 1L, 1L);
    }

    @Test
    void softDeleteBoqItem_approvedItem_throwsIllegalStateException() {
        BoqItem item = approvedItem(new BigDecimal("10.00"));
        when(boqItemRepository.findById(10L)).thenReturn(Optional.of(item));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());

        assertThatThrownBy(() -> boqService.softDeleteBoqItem(10L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only DRAFT items can be deleted");
    }

    // ── updateBoqItem ─────────────────────────────────────────────────────────

    @Test
    void updateBoqItem_validChanges_updatesDescriptionAndRate() {
        BoqItem item = draftItem();
        when(boqItemRepository.findById(10L)).thenReturn(Optional.of(item));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());
        when(boqItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateBoqItemRequest req = new UpdateBoqItemRequest(null, null, null, null,
                "Updated description", null, null, new BigDecimal("250.00"),
                null, null, null, null);

        boqService.updateBoqItem(10L, req, 1L);

        assertThat(item.getDescription()).isEqualTo("Updated description");
        assertThat(item.getUnitRate()).isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    void updateBoqItem_lockedItem_throwsIllegalStateException() {
        BoqItem item = approvedItem(new BigDecimal("10.00"));
        item.setStatus(BoqItemStatus.LOCKED);
        when(boqItemRepository.findById(10L)).thenReturn(Optional.of(item));
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());

        UpdateBoqItemRequest req = new UpdateBoqItemRequest(null, null, null, null,
                "New desc", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> boqService.updateBoqItem(10L, req, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only DRAFT items can be edited");
    }

    // ── getFinancialSummary ───────────────────────────────────────────────────

    @Test
    void getFinancialSummary_noItems_returnsZeroTotals() {
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());
        when(customerProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(boqItemRepository.getFinancialTotals(1L)).thenReturn(Collections.emptyList());
        when(boqItemRepository.getFinancialCategoryBreakdown(1L)).thenReturn(Collections.emptyList());
        when(boqItemRepository.getFinancialWorkTypeBreakdown(1L)).thenReturn(Collections.emptyList());

        BoqFinancialSummary summary = boqService.getFinancialSummary(1L, 1L);

        assertThat(summary.totalPlannedCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.totalExecutedCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.totalBilledCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.projectId()).isEqualTo(1L);
    }

    @Test
    void getFinancialSummary_withTotals_calculatesPercentagesCorrectly() {
        doNothing().when(projectAccessGuard).verifyPortalAccess(anyLong(), anyLong());
        when(customerProjectRepository.findById(1L)).thenReturn(Optional.of(project));

        // totals row: [totalItems, activeItems, plannedCost, executedCost, billedCost, costToComplete]
        Object[] row = {2L, 2L,
                new BigDecimal("100000.00"), new BigDecimal("50000.00"),
                new BigDecimal("25000.00"), new BigDecimal("50000.00")};
        when(boqItemRepository.getFinancialTotals(1L)).thenReturn(List.<Object[]>of(row));
        when(boqItemRepository.getFinancialCategoryBreakdown(1L)).thenReturn(Collections.emptyList());
        when(boqItemRepository.getFinancialWorkTypeBreakdown(1L)).thenReturn(Collections.emptyList());

        BoqFinancialSummary summary = boqService.getFinancialSummary(1L, 1L);

        // executionPercentage = 50000 / 100000 * 100 = 50%
        assertThat(summary.overallExecutionPercentage())
                .isEqualByComparingTo(new BigDecimal("50.0000"));
        // billingPercentage = 25000 / 50000 * 100 = 50%
        assertThat(summary.overallBillingPercentage())
                .isEqualByComparingTo(new BigDecimal("50.0000"));
    }

    // ── searchBoqItems (specification-based filter) ───────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void searchBoqItems_filterByProject_returnsPage() {
        BoqItem item = draftItem();
        Page<BoqItem> page = new PageImpl<>(List.of(item));
        when(boqItemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        BoqSearchFilter filter = new BoqSearchFilter();
        filter.setProjectId(1L);
        filter.setPage(0);
        filter.setSize(10);
        filter.setSortBy("id");
        filter.setSortDirection("asc");

        Page<BoqItemResponse> result = boqService.searchBoqItems(filter);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchBoqItems_filterByWorkType_passesSpecification() {
        Page<BoqItem> page = new PageImpl<>(Collections.emptyList());
        when(boqItemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        BoqSearchFilter filter = new BoqSearchFilter();
        filter.setWorkTypeId(5L);
        filter.setPage(0);
        filter.setSize(20);
        filter.setSortBy("id");
        filter.setSortDirection("asc");

        Page<BoqItemResponse> result = boqService.searchBoqItems(filter);

        assertThat(result).isNotNull();
        verify(boqItemRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
