package com.wd.api.service.dpc;

import com.wd.api.config.CompanyInfoConfig;
import com.wd.api.dto.dpc.DpcDocumentDto;
import com.wd.api.dto.dpc.DpcMasterCostSummaryDto;
import com.wd.api.model.BoqDocument;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.DpcCustomizationLine;
import com.wd.api.model.DpcDocument;
import com.wd.api.model.DpcDocumentScope;
import com.wd.api.model.DpcScopeTemplate;
import com.wd.api.model.enums.BoqDocumentStatus;
import com.wd.api.model.enums.DpcCustomizationSource;
import com.wd.api.model.enums.DpcDocumentStatus;
import com.wd.api.repository.BoqDocumentRepository;
import com.wd.api.repository.BoqItemRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.DpcCustomizationLineRepository;
import com.wd.api.repository.DpcDocumentRepository;
import com.wd.api.repository.DpcDocumentScopeRepository;
import com.wd.api.repository.DpcScopeOptionRepository;
import com.wd.api.repository.DpcScopeTemplateRepository;
import com.wd.api.repository.PaymentStageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-unit tests for {@link DpcDocumentService}.  All collaborators are
 * mocked, including {@link DpcCostRollupService} and
 * {@link DpcCustomizationService} — those have their own unit tests.
 */
@ExtendWith(MockitoExtension.class)
class DpcDocumentServiceTest {

    @Mock private DpcDocumentRepository dpcDocumentRepository;
    @Mock private DpcDocumentScopeRepository dpcDocumentScopeRepository;
    @Mock private DpcCustomizationLineRepository dpcCustomizationLineRepository;
    @Mock private DpcScopeTemplateRepository dpcScopeTemplateRepository;
    @Mock private DpcScopeOptionRepository dpcScopeOptionRepository;
    @Mock private BoqDocumentRepository boqDocumentRepository;
    @Mock private CustomerProjectRepository customerProjectRepository;
    @Mock private BoqItemRepository boqItemRepository;
    @Mock private PaymentStageRepository paymentStageRepository;
    @Mock private DpcCostRollupService costRollupService;
    @Mock private DpcCustomizationService customizationService;
    @Mock private CompanyInfoConfig companyInfoConfig;

    @InjectMocks
    private DpcDocumentService service;

    private CustomerProject project;
    private BoqDocument approvedBoq;

    @BeforeEach
    void setUp() {
        project = new CustomerProject();
        project.setId(1L);
        project.setName("Test Villa");

        approvedBoq = new BoqDocument();
        approvedBoq.setId(50L);
        approvedBoq.setProject(project);
        approvedBoq.setStatus(BoqDocumentStatus.APPROVED);

        // Lenient stubs — not every test exercises assemble().
        lenient().when(companyInfoConfig.getName()).thenReturn("Walldot Builders LLP");
        lenient().when(companyInfoConfig.getPhone()).thenReturn("+91 1234567890");
        lenient().when(dpcScopeTemplateRepository.findByIsActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of());
        lenient().when(dpcDocumentScopeRepository.findByDpcDocumentIdOrderByDisplayOrderAsc(anyLong()))
                .thenReturn(List.of());
        lenient().when(dpcCustomizationLineRepository.findByDpcDocumentIdOrderByDisplayOrderAsc(anyLong()))
                .thenReturn(List.of());
        lenient().when(paymentStageRepository.findByBoqDocumentIdOrderByStageNumberAsc(anyLong()))
                .thenReturn(List.of());
        lenient().when(costRollupService.computeForProject(anyLong(), anyLong()))
                .thenReturn(new DpcMasterCostSummaryDto(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        null, null, null, List.of()));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void create_throwsWhenProjectHasNoApprovedBoq() {
        when(customerProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(boqDocumentRepository.findFirstByProject_IdAndStatusOrderByApprovedAtDesc(
                1L, BoqDocumentStatus.APPROVED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(1L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED BoQ");
    }

    @Test
    void create_assignsRevisionOneForFirstThenIncrements() {
        when(customerProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(boqDocumentRepository.findFirstByProject_IdAndStatusOrderByApprovedAtDesc(
                1L, BoqDocumentStatus.APPROVED)).thenReturn(Optional.of(approvedBoq));
        when(dpcDocumentRepository.save(any(DpcDocument.class)))
                .thenAnswer(inv -> { DpcDocument d = inv.getArgument(0); d.setId(900L); return d; });
        when(dpcDocumentRepository.findById(900L))
                .thenAnswer(inv -> Optional.of(savedDpc(900L, DpcDocumentStatus.DRAFT, 1)));

        // First DPC: no existing revisions.
        when(dpcDocumentRepository.findMaxRevisionByProjectId(1L)).thenReturn(Optional.empty());
        ArgumentCaptor<DpcDocument> captor = ArgumentCaptor.forClass(DpcDocument.class);

        service.create(1L, 99L);

        verify(dpcDocumentRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getRevisionNumber()).isEqualTo(1);

        // Second DPC: max revision is 1, so next is 2.
        when(dpcDocumentRepository.findMaxRevisionByProjectId(1L)).thenReturn(Optional.of(1));
        ArgumentCaptor<DpcDocument> captor2 = ArgumentCaptor.forClass(DpcDocument.class);

        service.create(1L, 99L);

        verify(dpcDocumentRepository, atLeastOnce()).save(captor2.capture());
        DpcDocument lastSave = captor2.getAllValues().get(captor2.getAllValues().size() - 1);
        assertThat(lastSave.getRevisionNumber()).isEqualTo(2);
    }

    @Test
    void issue_stampsTimestampUserAndPdfThenLocks() {
        DpcDocument draft = savedDpc(900L, DpcDocumentStatus.DRAFT, 1);
        when(dpcDocumentRepository.findById(900L)).thenReturn(Optional.of(draft));

        DpcDocumentDto dto = service.issue(900L, 42L, new byte[]{1, 2, 3}, 555L);

        assertThat(draft.getStatus()).isEqualTo(DpcDocumentStatus.ISSUED);
        assertThat(draft.getIssuedByUserId()).isEqualTo(42L);
        assertThat(draft.getIssuedPdfDocumentId()).isEqualTo(555L);
        assertThat(draft.getIssuedAt()).isNotNull();
        assertThat(draft.isLocked()).isTrue();
        assertThat(dto.status()).isEqualTo(DpcDocumentStatus.ISSUED.name());

        // Re-issue must be rejected.
        assertThatThrownBy(() -> service.issue(900L, 42L, new byte[0], 999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already ISSUED");
    }

    @Test
    void createNewRevision_copiesScopesRefreshesAutoAndCarriesManualForward() {
        DpcDocument prev = savedDpc(900L, DpcDocumentStatus.ISSUED, 1);
        prev.setBranchManagerName("Branch X");
        prev.setClientSignatoryName("Mr. Customer");

        DpcScopeTemplate template = new DpcScopeTemplate();
        template.setId(101L);
        template.setCode("FOUNDATION");

        DpcDocumentScope prevScope = new DpcDocumentScope();
        prevScope.setId(7777L);
        prevScope.setDpcDocument(prev);
        prevScope.setScopeTemplate(template);
        prevScope.setSelectedOptionRationale("Stable soil");
        prevScope.setIncludedInPdf(true);
        prevScope.setDisplayOrder(1);

        DpcCustomizationLine autoLine = new DpcCustomizationLine();
        autoLine.setId(31L);
        autoLine.setSource(DpcCustomizationSource.AUTO_FROM_BOQ_ADDON);
        autoLine.setTitle("Old auto line");
        autoLine.setAmount(new BigDecimal("100"));

        DpcCustomizationLine manualLine = new DpcCustomizationLine();
        manualLine.setId(32L);
        manualLine.setSource(DpcCustomizationSource.MANUAL);
        manualLine.setTitle("Custom marble");
        manualLine.setAmount(new BigDecimal("80000"));

        when(dpcDocumentRepository.findById(900L)).thenReturn(Optional.of(prev));
        when(dpcDocumentRepository.save(any(DpcDocument.class)))
                .thenAnswer(inv -> { DpcDocument d = inv.getArgument(0); if (d.getId() == null) d.setId(901L); return d; });
        when(dpcDocumentScopeRepository.findByDpcDocumentIdOrderByDisplayOrderAsc(900L))
                .thenReturn(List.of(prevScope));
        when(dpcCustomizationLineRepository.findByDpcDocumentIdOrderByDisplayOrderAsc(900L))
                .thenReturn(List.of(autoLine, manualLine));
        // createNewRevision returns getById(newId) at the end; stub the new doc
        // load so the assemble path can complete.
        when(dpcDocumentRepository.findById(901L)).thenAnswer(inv -> {
            DpcDocument fresh = savedDpc(901L, DpcDocumentStatus.DRAFT, 2);
            fresh.setBranchManagerName("Branch X");
            fresh.setClientSignatoryName("Mr. Customer");
            return Optional.of(fresh);
        });

        service.createNewRevision(900L, 42L);

        // Carries header forward.
        ArgumentCaptor<DpcDocument> dpcCaptor = ArgumentCaptor.forClass(DpcDocument.class);
        verify(dpcDocumentRepository, atLeastOnce()).save(dpcCaptor.capture());
        DpcDocument newDpc = dpcCaptor.getAllValues().get(0);
        assertThat(newDpc.getRevisionNumber()).isEqualTo(2);
        assertThat(newDpc.getStatus()).isEqualTo(DpcDocumentStatus.DRAFT);
        assertThat(newDpc.getBranchManagerName()).isEqualTo("Branch X");
        assertThat(newDpc.getClientSignatoryName()).isEqualTo("Mr. Customer");

        // Scope rows copied (one row, populated from previous).
        ArgumentCaptor<List<DpcDocumentScope>> scopeCaptor = ArgumentCaptor.forClass(List.class);
        verify(dpcDocumentScopeRepository).saveAll(scopeCaptor.capture());
        assertThat(scopeCaptor.getValue()).hasSize(1);
        assertThat(scopeCaptor.getValue().get(0).getSelectedOptionRationale()).isEqualTo("Stable soil");

        // AUTO refresh delegated to the customization service.
        verify(customizationService).autoPopulateFromBoq(any(DpcDocument.class));

        // MANUAL line carried forward, AUTO line dropped.
        ArgumentCaptor<List<DpcCustomizationLine>> lineCaptor = ArgumentCaptor.forClass(List.class);
        verify(dpcCustomizationLineRepository).saveAll(lineCaptor.capture());
        List<DpcCustomizationLine> carried = lineCaptor.getValue();
        assertThat(carried).hasSize(1);
        assertThat(carried.get(0).getSource()).isEqualTo(DpcCustomizationSource.MANUAL);
        assertThat(carried.get(0).getTitle()).isEqualTo("Custom marble");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DpcDocument savedDpc(Long id, DpcDocumentStatus status, int revision) {
        DpcDocument d = new DpcDocument();
        d.setId(id);
        d.setProject(project);
        d.setBoqDocument(approvedBoq);
        d.setStatus(status);
        d.setRevisionNumber(revision);
        return d;
    }
}
