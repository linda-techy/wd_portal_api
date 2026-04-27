package com.wd.api.service.dpc;

import com.wd.api.dto.dpc.CreateDpcCustomizationCatalogItemRequest;
import com.wd.api.dto.dpc.DpcCustomizationCatalogItemDto;
import com.wd.api.dto.dpc.PromoteCustomizationToCatalogRequest;
import com.wd.api.model.DpcCustomizationCatalogItem;
import com.wd.api.model.DpcCustomizationLine;
import com.wd.api.model.enums.DpcCustomizationSource;
import com.wd.api.repository.DpcCustomizationCatalogRepository;
import com.wd.api.repository.DpcCustomizationLineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the "promote ad-hoc DPC customization to catalog" workflow.
 *
 * Mirrors {@code QuotationCatalogPromotionServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class DpcCustomizationCatalogPromotionServiceTest {

    @Mock private DpcCustomizationCatalogRepository catalogRepository;
    @Mock private DpcCustomizationLineRepository customizationLineRepository;
    @Mock private DpcCustomizationCatalogService catalogService;

    @InjectMocks private DpcCustomizationCatalogPromotionService service;

    private DpcCustomizationLine adHocLine;

    @BeforeEach
    void setUp() {
        adHocLine = new DpcCustomizationLine();
        adHocLine.setId(42L);
        adHocLine.setTitle("One-off custom thing");
        adHocLine.setDescription("Some notes about the customization");
        adHocLine.setAmount(new BigDecimal("1234.560000"));
        adHocLine.setSource(DpcCustomizationSource.MANUAL);
        adHocLine.setDisplayOrder(3);
        adHocLine.setCatalogItem(null); // ad-hoc
    }

    @Test
    void promote_createsCatalogRow_andLinksLine() {
        DpcCustomizationCatalogItemDto createdDto = new DpcCustomizationCatalogItemDto(
                99L, "CUSTOM-ITEM", "Custom Item", "Some notes about the customization",
                "Elevation", "lot", new BigDecimal("1234.560000"), 0, true,
                LocalDateTime.now(), LocalDateTime.now());

        DpcCustomizationCatalogItem savedEntity = new DpcCustomizationCatalogItem();
        savedEntity.setId(99L);
        savedEntity.setCode("CUSTOM-ITEM");

        PromoteCustomizationToCatalogRequest req = new PromoteCustomizationToCatalogRequest(
                "CUSTOM-ITEM", "Custom Item", "Elevation", "lot", new BigDecimal("1234.560000"));

        when(customizationLineRepository.findById(42L)).thenReturn(Optional.of(adHocLine));
        when(catalogService.create(any(CreateDpcCustomizationCatalogItemRequest.class))).thenReturn(createdDto);
        when(catalogRepository.findById(99L)).thenReturn(Optional.of(savedEntity));

        DpcCustomizationCatalogItemDto result = service.promoteAdHocCustomization(42L, req);

        assertThat(result.id()).isEqualTo(99L);
        assertThat(result.code()).isEqualTo("CUSTOM-ITEM");

        ArgumentCaptor<DpcCustomizationLine> lineCaptor = ArgumentCaptor.forClass(DpcCustomizationLine.class);
        verify(customizationLineRepository).save(lineCaptor.capture());
        assertThat(lineCaptor.getValue().getCatalogItem()).isSameAs(savedEntity);
    }

    @Test
    void promote_throwsWhenLineAlreadyLinked() {
        DpcCustomizationCatalogItem alreadyLinked = new DpcCustomizationCatalogItem();
        alreadyLinked.setId(7L);
        adHocLine.setCatalogItem(alreadyLinked);

        when(customizationLineRepository.findById(42L)).thenReturn(Optional.of(adHocLine));

        PromoteCustomizationToCatalogRequest req = new PromoteCustomizationToCatalogRequest(
                "X", "X", null, null, null);

        assertThatThrownBy(() -> service.promoteAdHocCustomization(42L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already linked")
                .hasMessageContaining("7");

        verify(catalogService, never()).create(any());
        verify(customizationLineRepository, never()).save(any());
    }

    @Test
    void promote_derivesCodeFromNameWhenBlank() {
        DpcCustomizationCatalogItemDto createdDto = new DpcCustomizationCatalogItemDto(
                100L, "CUSTOM-ITEM-WITH-SPACES", "Custom item with spaces!", null,
                null, null, new BigDecimal("1234.560000"), 0, true,
                LocalDateTime.now(), LocalDateTime.now());
        DpcCustomizationCatalogItem savedEntity = new DpcCustomizationCatalogItem();
        savedEntity.setId(100L);

        when(customizationLineRepository.findById(42L)).thenReturn(Optional.of(adHocLine));
        when(catalogService.create(any(CreateDpcCustomizationCatalogItemRequest.class))).thenReturn(createdDto);
        when(catalogRepository.findById(100L)).thenReturn(Optional.of(savedEntity));

        // Blank code -> derive from name; null defaultAmount -> falls back to line's amount.
        PromoteCustomizationToCatalogRequest req = new PromoteCustomizationToCatalogRequest(
                "  ", "Custom item with spaces!", null, null, null);

        service.promoteAdHocCustomization(42L, req);

        ArgumentCaptor<CreateDpcCustomizationCatalogItemRequest> reqCaptor =
                ArgumentCaptor.forClass(CreateDpcCustomizationCatalogItemRequest.class);
        verify(catalogService).create(reqCaptor.capture());

        // Derived code: "Custom item with spaces!" -> "CUSTOM-ITEM-WITH-SPACES"
        assertThat(reqCaptor.getValue().code()).isEqualTo("CUSTOM-ITEM-WITH-SPACES");
        // defaultAmount null in request -> falls back to source line's amount.
        assertThat(reqCaptor.getValue().defaultAmount()).isEqualByComparingTo("1234.560000");
    }

    @Test
    void promote_throwsWhenLineNotFound() {
        when(customizationLineRepository.findById(404L)).thenReturn(Optional.empty());

        PromoteCustomizationToCatalogRequest req = new PromoteCustomizationToCatalogRequest(
                null, "Some name", null, null, null);

        assertThatThrownBy(() -> service.promoteAdHocCustomization(404L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("404");

        verify(catalogService, never()).create(any());
        verify(customizationLineRepository, never()).save(any());
    }
}
