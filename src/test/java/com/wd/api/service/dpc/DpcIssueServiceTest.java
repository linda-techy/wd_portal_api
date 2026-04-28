package com.wd.api.service.dpc;

import com.wd.api.dto.dpc.DpcDocumentDto;
import com.wd.api.dto.dpc.DpcMasterCostSummaryDto;
import com.wd.api.repository.DocumentCategoryRepository;
import com.wd.api.repository.DocumentRepository;
import com.wd.api.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DpcIssueService}'s pre-issue defensive checks.
 *
 * <p>Regression: a DPC was issued (project 47, REV 01) where every scope
 * rolled up to zero rupees because the BoQ items had no totalAmount mapped
 * to scopes. The customer-facing PDF showed "INR 0" everywhere — a trust
 * disaster. The fix is a precondition that refuses to issue a DPC whose
 * customized total is zero.
 */
@ExtendWith(MockitoExtension.class)
class DpcIssueServiceTest {

    @Mock private DpcRenderService dpcRenderService;
    @Mock private DpcDocumentService dpcDocumentService;
    @Mock private FileStorageService fileStorageService;
    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentCategoryRepository documentCategoryRepository;

    @InjectMocks
    private DpcIssueService service;

    @Test
    void issueAndPersist_blocksWhenTotalCustomizedIsZero() {
        DpcDocumentDto dto = stubDtoWithCustomizedTotal(BigDecimal.ZERO);
        when(dpcDocumentService.getById(123L)).thenReturn(dto);

        assertThatThrownBy(() -> service.issueAndPersist(123L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("zero")
                .hasMessageContaining("customized total");

        // Fail fast — never spend cycles rendering a zero-rupee PDF.
        verify(dpcRenderService, never()).renderPdf(any());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void issueAndPersist_blocksWhenMasterCostSummaryIsNull() {
        DpcDocumentDto dto = stubDtoWithCustomizedTotal(null); // null summary
        when(dpcDocumentService.getById(123L)).thenReturn(dto);

        assertThatThrownBy(() -> service.issueAndPersist(123L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("customized total");

        verify(dpcRenderService, never()).renderPdf(any());
    }

    @Test
    void issueAndPersist_blocksWhenTotalCustomizedIsNegative() {
        DpcDocumentDto dto = stubDtoWithCustomizedTotal(new BigDecimal("-1.00"));
        when(dpcDocumentService.getById(123L)).thenReturn(dto);

        assertThatThrownBy(() -> service.issueAndPersist(123L, 1L))
                .isInstanceOf(IllegalStateException.class);

        verify(dpcRenderService, never()).renderPdf(any());
    }

    /**
     * Build a DpcDocumentDto with the given total-customized rupee amount.
     * Pass {@code null} to simulate a missing master-cost summary.
     */
    private DpcDocumentDto stubDtoWithCustomizedTotal(BigDecimal totalCustomized) {
        DpcMasterCostSummaryDto summary = totalCustomized == null ? null
                : new DpcMasterCostSummaryDto(
                        BigDecimal.ZERO,        // totalOriginal
                        totalCustomized,         // totalCustomized
                        BigDecimal.ZERO,        // totalVariance
                        null, null, null,
                        List.of()
                );
        return new DpcDocumentDto(
                123L, 47L, 50L, 1, "DRAFT",
                null, null,
                "Demo Villa Project", "Mumbai", "Maharashtra", "Mumbai", "VILLA",
                BigDecimal.ZERO, 99L,
                "Demo Customer", "Walldot Authorised",
                null, "Branch", "+91 9876543210", "CRM", "+91 9876543210",
                null, null, null,
                List.of(), List.of(), summary, List.of(),
                null, null
        );
    }
}
