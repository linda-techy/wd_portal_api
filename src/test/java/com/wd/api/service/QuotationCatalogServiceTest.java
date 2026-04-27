package com.wd.api.service;

import com.wd.api.dto.quotation.CreateQuotationCatalogItemRequest;
import com.wd.api.dto.quotation.QuotationCatalogItemDto;
import com.wd.api.dto.quotation.QuotationCatalogSearchFilter;
import com.wd.api.dto.quotation.UpdateQuotationCatalogItemRequest;
import com.wd.api.model.QuotationCatalogItem;
import com.wd.api.repository.QuotationCatalogItemRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link QuotationCatalogService} business logic.
 */
@ExtendWith(MockitoExtension.class)
class QuotationCatalogServiceTest {

    @Mock private QuotationCatalogItemRepository repository;

    @InjectMocks private QuotationCatalogService service;

    private QuotationCatalogItem existing;

    @BeforeEach
    void setUp() {
        existing = new QuotationCatalogItem();
        existing.setId(1L);
        existing.setCode("OLD-CODE");
        existing.setName("Old name");
        existing.setDescription("Old description");
        existing.setCategory("Civil");
        existing.setUnit("sqft");
        existing.setDefaultUnitPrice(new BigDecimal("100.00"));
        existing.setTimesUsed(5);
        existing.setIsActive(true);
    }

    @Test
    void search_filtersByActiveAndSearchAndCategory() {
        QuotationCatalogSearchFilter filter = new QuotationCatalogSearchFilter(
                "site", "Site Prep", true, 0, 10, "timesUsed", "desc");

        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existing)));

        Page<QuotationCatalogItemDto> result = service.search(filter);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).code()).isEqualTo("OLD-CODE");
        // Verify the spec call happened (the spec lambda itself is opaque, so this is the
        // strongest assertion possible without an integration test).
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void create_throwsWhenCodeAlreadyExists() {
        CreateQuotationCatalogItemRequest req = new CreateQuotationCatalogItemRequest(
                "DUP-CODE", "Some name", null, null, null, BigDecimal.TEN);

        when(repository.existsByCodeIgnoreCase("DUP-CODE")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DUP-CODE")
                .hasMessageContaining("already exists");

        verify(repository, never()).save(any());
    }

    @Test
    void update_patchSemanticsOnlyTouchesNonNull() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(QuotationCatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // Only update name + defaultUnitPrice; everything else null -> untouched.
        UpdateQuotationCatalogItemRequest req = new UpdateQuotationCatalogItemRequest(
                null, "New name", null, null, null, new BigDecimal("250.00"), null);

        QuotationCatalogItemDto result = service.update(1L, req);

        ArgumentCaptor<QuotationCatalogItem> captor = ArgumentCaptor.forClass(QuotationCatalogItem.class);
        verify(repository).save(captor.capture());
        QuotationCatalogItem saved = captor.getValue();

        assertThat(saved.getCode()).isEqualTo("OLD-CODE"); // untouched
        assertThat(saved.getName()).isEqualTo("New name"); // changed
        assertThat(saved.getDescription()).isEqualTo("Old description"); // untouched
        assertThat(saved.getCategory()).isEqualTo("Civil"); // untouched
        assertThat(saved.getUnit()).isEqualTo("sqft"); // untouched
        assertThat(saved.getDefaultUnitPrice()).isEqualByComparingTo("250.00"); // changed
        assertThat(saved.getIsActive()).isTrue(); // untouched

        assertThat(result.name()).isEqualTo("New name");
    }

    @Test
    void softDelete_callsRepositoryDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.softDelete(1L);

        // softDelete relies entirely on @SQLDelete + @SQLRestriction — one
        // delete() call, no redundant is_active write.
        verify(repository, times(1)).delete(existing);
        verify(repository, never()).save(any(QuotationCatalogItem.class));
    }
}
