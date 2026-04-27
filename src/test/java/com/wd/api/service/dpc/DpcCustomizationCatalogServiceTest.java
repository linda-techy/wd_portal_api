package com.wd.api.service.dpc;

import com.wd.api.dto.dpc.CreateDpcCustomizationCatalogItemRequest;
import com.wd.api.dto.dpc.DpcCustomizationCatalogItemDto;
import com.wd.api.dto.dpc.DpcCustomizationCatalogSearchFilter;
import com.wd.api.dto.dpc.UpdateDpcCustomizationCatalogItemRequest;
import com.wd.api.model.DpcCustomizationCatalogItem;
import com.wd.api.repository.DpcCustomizationCatalogRepository;
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
 * Unit tests for {@link DpcCustomizationCatalogService} business logic.
 *
 * Mirrors {@code QuotationCatalogServiceTest} — same shape, just renamed and
 * with {@code defaultAmount} in place of {@code defaultUnitPrice}.
 */
@ExtendWith(MockitoExtension.class)
class DpcCustomizationCatalogServiceTest {

    @Mock private DpcCustomizationCatalogRepository repository;

    @InjectMocks private DpcCustomizationCatalogService service;

    private DpcCustomizationCatalogItem existing;

    @BeforeEach
    void setUp() {
        existing = new DpcCustomizationCatalogItem();
        existing.setId(1L);
        existing.setCode("OLD-CODE");
        existing.setName("Old name");
        existing.setDescription("Old description");
        existing.setCategory("Elevation");
        existing.setUnit("sqft");
        existing.setDefaultAmount(new BigDecimal("100.000000"));
        existing.setTimesUsed(5);
        existing.setIsActive(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_filtersByActiveAndSearchAndCategory() {
        DpcCustomizationCatalogSearchFilter filter = new DpcCustomizationCatalogSearchFilter(
                "balcony", "Elevation", true, 0, 10, "timesUsed", "desc");

        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existing)));

        Page<DpcCustomizationCatalogItemDto> result = service.search(filter);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).code()).isEqualTo("OLD-CODE");
        // The spec lambda itself is opaque; the strongest assertion possible
        // without an integration test is that the spec was passed through.
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void create_throwsWhenCodeAlreadyExists() {
        CreateDpcCustomizationCatalogItemRequest req = new CreateDpcCustomizationCatalogItemRequest(
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
        when(repository.save(any(DpcCustomizationCatalogItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Only update name + defaultAmount; everything else null -> untouched.
        UpdateDpcCustomizationCatalogItemRequest req = new UpdateDpcCustomizationCatalogItemRequest(
                null, "New name", null, null, null, new BigDecimal("250.000000"), null);

        DpcCustomizationCatalogItemDto result = service.update(1L, req);

        ArgumentCaptor<DpcCustomizationCatalogItem> captor = ArgumentCaptor.forClass(DpcCustomizationCatalogItem.class);
        verify(repository).save(captor.capture());
        DpcCustomizationCatalogItem saved = captor.getValue();

        assertThat(saved.getCode()).isEqualTo("OLD-CODE"); // untouched
        assertThat(saved.getName()).isEqualTo("New name"); // changed
        assertThat(saved.getDescription()).isEqualTo("Old description"); // untouched
        assertThat(saved.getCategory()).isEqualTo("Elevation"); // untouched
        assertThat(saved.getUnit()).isEqualTo("sqft"); // untouched
        assertThat(saved.getDefaultAmount()).isEqualByComparingTo("250.000000"); // changed
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
        verify(repository, never()).save(any(DpcCustomizationCatalogItem.class));
    }
}
