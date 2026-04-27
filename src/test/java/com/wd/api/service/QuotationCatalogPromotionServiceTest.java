package com.wd.api.service;

import com.wd.api.dto.quotation.CreateQuotationCatalogItemRequest;
import com.wd.api.dto.quotation.PromoteItemToCatalogRequest;
import com.wd.api.dto.quotation.QuotationCatalogItemDto;
import com.wd.api.model.LeadQuotationItem;
import com.wd.api.model.QuotationCatalogItem;
import com.wd.api.repository.LeadQuotationItemRepository;
import com.wd.api.repository.QuotationCatalogItemRepository;
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
 * Unit tests for the "promote ad-hoc item to catalog" workflow.
 */
@ExtendWith(MockitoExtension.class)
class QuotationCatalogPromotionServiceTest {

    @Mock private QuotationCatalogItemRepository catalogRepository;
    @Mock private LeadQuotationItemRepository quotationItemRepository;
    @Mock private QuotationCatalogService catalogService;

    @InjectMocks private QuotationCatalogPromotionService service;

    private LeadQuotationItem adHocItem;

    @BeforeEach
    void setUp() {
        adHocItem = new LeadQuotationItem();
        adHocItem.setId(42L);
        adHocItem.setItemNumber(1);
        adHocItem.setDescription("One-off custom thing");
        adHocItem.setQuantity(BigDecimal.ONE);
        adHocItem.setUnitPrice(new BigDecimal("1234.56"));
        adHocItem.setTotalPrice(new BigDecimal("1234.56"));
        adHocItem.setCatalogItem(null); // ad-hoc
    }

    @Test
    void promote_createsCatalogRow_andLinksItem() {
        QuotationCatalogItemDto createdDto = new QuotationCatalogItemDto(
                99L, "CUSTOM-ITEM", "Custom Item", "One-off custom thing",
                "Civil", "lot", new BigDecimal("1234.56"), 0, true,
                LocalDateTime.now(), LocalDateTime.now());

        QuotationCatalogItem savedEntity = new QuotationCatalogItem();
        savedEntity.setId(99L);
        savedEntity.setCode("CUSTOM-ITEM");

        PromoteItemToCatalogRequest req = new PromoteItemToCatalogRequest(
                "CUSTOM-ITEM", "Custom Item", "Civil", "lot", new BigDecimal("1234.56"));

        when(quotationItemRepository.findById(42L)).thenReturn(Optional.of(adHocItem));
        when(catalogService.create(any(CreateQuotationCatalogItemRequest.class))).thenReturn(createdDto);
        when(catalogRepository.findById(99L)).thenReturn(Optional.of(savedEntity));

        QuotationCatalogItemDto result = service.promoteAdHocItem(42L, req);

        assertThat(result.id()).isEqualTo(99L);
        assertThat(result.code()).isEqualTo("CUSTOM-ITEM");

        ArgumentCaptor<LeadQuotationItem> itemCaptor = ArgumentCaptor.forClass(LeadQuotationItem.class);
        verify(quotationItemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getCatalogItem()).isSameAs(savedEntity);
    }

    @Test
    void promote_throwsWhenItemAlreadyLinked() {
        QuotationCatalogItem alreadyLinked = new QuotationCatalogItem();
        alreadyLinked.setId(7L);
        adHocItem.setCatalogItem(alreadyLinked);

        when(quotationItemRepository.findById(42L)).thenReturn(Optional.of(adHocItem));

        PromoteItemToCatalogRequest req = new PromoteItemToCatalogRequest(
                "X", "X", null, null, null);

        assertThatThrownBy(() -> service.promoteAdHocItem(42L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already linked")
                .hasMessageContaining("7");

        verify(catalogService, never()).create(any());
        verify(quotationItemRepository, never()).save(any());
    }

    @Test
    void promote_derivesCodeFromNameWhenBlank() {
        QuotationCatalogItemDto createdDto = new QuotationCatalogItemDto(
                100L, "CUSTOM-ITEM-WITH-SPACES", "Custom item with spaces!", null,
                null, null, new BigDecimal("1234.56"), 0, true,
                LocalDateTime.now(), LocalDateTime.now());
        QuotationCatalogItem savedEntity = new QuotationCatalogItem();
        savedEntity.setId(100L);

        when(quotationItemRepository.findById(42L)).thenReturn(Optional.of(adHocItem));
        when(catalogService.create(any(CreateQuotationCatalogItemRequest.class))).thenReturn(createdDto);
        when(catalogRepository.findById(100L)).thenReturn(Optional.of(savedEntity));

        // Blank code -> derive from name.
        PromoteItemToCatalogRequest req = new PromoteItemToCatalogRequest(
                "  ", "Custom item with spaces!", null, null, null);

        service.promoteAdHocItem(42L, req);

        ArgumentCaptor<CreateQuotationCatalogItemRequest> reqCaptor =
                ArgumentCaptor.forClass(CreateQuotationCatalogItemRequest.class);
        verify(catalogService).create(reqCaptor.capture());

        // Derived code: "Custom item with spaces!" -> "CUSTOM-ITEM-WITH-SPACES"
        assertThat(reqCaptor.getValue().code()).isEqualTo("CUSTOM-ITEM-WITH-SPACES");
        // defaultUnitPrice null in request -> falls back to source line item's unitPrice.
        assertThat(reqCaptor.getValue().defaultUnitPrice()).isEqualByComparingTo("1234.56");
    }
}
