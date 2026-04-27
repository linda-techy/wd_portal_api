package com.wd.api.service.dpc;

import com.wd.api.dto.dpc.UpsertCustomizationLineRequest;
import com.wd.api.model.BoqDocument;
import com.wd.api.model.BoqItem;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.DpcCustomizationLine;
import com.wd.api.model.DpcDocument;
import com.wd.api.model.enums.DpcCustomizationSource;
import com.wd.api.model.enums.DpcDocumentStatus;
import com.wd.api.model.enums.ItemKind;
import com.wd.api.repository.BoqItemRepository;
import com.wd.api.repository.DpcCustomizationLineRepository;
import com.wd.api.repository.DpcDocumentRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-unit tests for {@link DpcCustomizationService}.  Verifies
 * regeneration semantics, MANUAL row creation, and the AUTO/locked guards.
 */
@ExtendWith(MockitoExtension.class)
class DpcCustomizationServiceTest {

    @Mock private DpcDocumentRepository dpcDocumentRepository;
    @Mock private DpcCustomizationLineRepository dpcCustomizationLineRepository;
    @Mock private BoqItemRepository boqItemRepository;
    @Mock private com.wd.api.repository.DpcCustomizationCatalogRepository dpcCustomizationCatalogRepository;
    @Mock private DpcCustomizationCatalogService dpcCustomizationCatalogService;

    @InjectMocks
    private DpcCustomizationService service;

    private CustomerProject project;
    private BoqDocument boq;
    private DpcDocument dpc;

    @BeforeEach
    void setUp() {
        project = new CustomerProject();
        project.setId(1L);

        boq = new BoqDocument();
        boq.setId(50L);

        dpc = new DpcDocument();
        dpc.setId(900L);
        dpc.setProject(project);
        dpc.setBoqDocument(boq);
        dpc.setStatus(DpcDocumentStatus.DRAFT);
    }

    private BoqItem addon(long id, String description, BigDecimal amount) {
        BoqItem item = new BoqItem();
        item.setId(id);
        item.setProject(project);
        item.setBoqDocument(boq);
        item.setDescription(description);
        item.setSpecifications("spec-" + id);
        item.setItemKind(ItemKind.ADDON);
        item.setTotalAmount(amount);
        item.setIsActive(true);
        return item;
    }

    private BoqItem base(long id) {
        BoqItem item = addon(id, "base item", new BigDecimal("999"));
        item.setItemKind(ItemKind.BASE);
        return item;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void autoPopulateFromBoq_createsOneLinePerAddon_skippingBase() {
        BoqItem a1 = addon(11L, "Premium Tiles Upgrade", new BigDecimal("15000"));
        BoqItem a2 = addon(12L, "Solar Geyser Add-on",   new BigDecimal("25000"));
        BoqItem b1 = base(13L);
        when(boqItemRepository.findByProjectIdWithAssociations(1L))
                .thenReturn(List.of(a1, b1, a2));

        service.autoPopulateFromBoq(dpc);

        verify(dpcCustomizationLineRepository)
                .deleteByDpcDocumentIdAndSource(900L, DpcCustomizationSource.AUTO_FROM_BOQ_ADDON);

        ArgumentCaptor<List<DpcCustomizationLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(dpcCustomizationLineRepository).saveAll(captor.capture());

        List<DpcCustomizationLine> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(l -> l.getSource() == DpcCustomizationSource.AUTO_FROM_BOQ_ADDON);
        assertThat(saved).extracting(DpcCustomizationLine::getBoqItemId).containsExactlyInAnyOrder(11L, 12L);
        assertThat(saved.get(0).getDisplayOrder()).isZero();
        assertThat(saved.get(1).getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void addManualLine_setsSourceManualAndPersists() {
        when(dpcDocumentRepository.findById(900L)).thenReturn(Optional.of(dpc));
        when(dpcCustomizationLineRepository.findByDpcDocumentIdOrderByDisplayOrderAsc(900L))
                .thenReturn(List.of());
        when(dpcCustomizationLineRepository.save(any(DpcCustomizationLine.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpsertCustomizationLineRequest req = new UpsertCustomizationLineRequest(
                null, "Custom Marble Flooring", "Imported Italian marble", new BigDecimal("80000"));

        DpcCustomizationLine result = service.addManualLine(900L, req);

        assertThat(result.getSource()).isEqualTo(DpcCustomizationSource.MANUAL);
        assertThat(result.getTitle()).isEqualTo("Custom Marble Flooring");
        assertThat(result.getAmount()).isEqualByComparingTo("80000");
        assertThat(result.getDisplayOrder()).isZero();
    }

    @Test
    void deleteLine_throwsWhenSourceIsAuto() {
        DpcCustomizationLine auto = new DpcCustomizationLine();
        auto.setId(77L);
        auto.setDpcDocument(dpc);
        auto.setSource(DpcCustomizationSource.AUTO_FROM_BOQ_ADDON);
        when(dpcCustomizationLineRepository.findById(77L)).thenReturn(Optional.of(auto));

        assertThatThrownBy(() -> service.deleteLine(77L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Auto lines refresh from BoQ");

        verify(dpcCustomizationLineRepository, never()).delete(any(DpcCustomizationLine.class));
    }

    @Test
    void addManualLine_throwsWhenDpcIsLocked() {
        dpc.setStatus(DpcDocumentStatus.ISSUED);
        when(dpcDocumentRepository.findById(900L)).thenReturn(Optional.of(dpc));

        UpsertCustomizationLineRequest req =
                new UpsertCustomizationLineRequest(null, "anything", null, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.addManualLine(900L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ISSUED");

        verify(dpcCustomizationLineRepository, never()).save(any(DpcCustomizationLine.class));
    }
}
