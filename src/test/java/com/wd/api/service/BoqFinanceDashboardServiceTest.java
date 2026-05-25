package com.wd.api.service;

import com.wd.api.factory.ChangeOrderTestFactory;
import com.wd.api.model.ChangeOrder;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.enums.ChangeOrderStatus;
import com.wd.api.model.enums.ChangeOrderType;
import com.wd.api.model.enums.VOCategory;
import com.wd.api.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoqFinanceDashboardServiceTest {

    @Mock BoqDocumentRepository boqDocumentRepository;
    @Mock PaymentStageRepository stageRepository;
    @Mock ChangeOrderRepository changeOrderRepository;
    @Mock BoqInvoiceRepository invoiceRepository;
    @Mock CreditNoteRepository creditNoteRepository;
    @Mock RefundNoticeRepository refundNoticeRepository;

    private BoqFinanceDashboardService service() {
        return new BoqFinanceDashboardService(boqDocumentRepository, stageRepository,
                changeOrderRepository, invoiceRepository, creditNoteRepository, refundNoticeRepository);
    }

    private ChangeOrder approved(ChangeOrderType type, String inclGst, VOCategory voCat) {
        CustomerProject p = new CustomerProject();
        p.setId(50L);
        ChangeOrder co = ChangeOrderTestFactory.create(p);
        co.setCoType(type);
        co.setStatus(ChangeOrderStatus.APPROVED);
        co.setNetAmountInclGst(new BigDecimal(inclGst));
        co.setVoCategory(voCat); // null = regular CO, non-null = variation order (still a change_orders row)
        return co;
    }

    @Test
    void summary_sumsOnlyChangeOrders_additionsMinusReductions() {
        when(boqDocumentRepository.findApprovedByProjectId(50L))
                .thenReturn(java.util.Optional.empty()); // originalContractValue = 0
        when(changeOrderRepository.findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(50L))
                .thenReturn(List.of(
                        approved(ChangeOrderType.SCOPE_ADDITION, "1000", null),
                        approved(ChangeOrderType.SCOPE_REDUCTION, "300", null)));
        stubZeroInvoicingAndCredits();

        var s = service().getSummary(50L);

        assertThat(s.coAdditions()).isEqualByComparingTo("1000");
        assertThat(s.coReductions()).isEqualByComparingTo("300");
        assertThat(s.netProjectValue()).isEqualByComparingTo("700");
    }

    @Test
    void summary_countsVariationOrderRowExactlyOnce() {
        // A VO is a change_orders row with voCategory set; it must be counted once, not excluded, not doubled.
        when(boqDocumentRepository.findApprovedByProjectId(50L))
                .thenReturn(java.util.Optional.empty());
        when(changeOrderRepository.findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(50L))
                .thenReturn(List.of(approved(ChangeOrderType.SCOPE_ADDITION, "500", VOCategory.MATERIAL_HEAVY)));
        stubZeroInvoicingAndCredits();

        var s = service().getSummary(50L);

        assertThat(s.coAdditions()).isEqualByComparingTo("500");
    }

    @Test
    void service_hasNoProjectVariationDependency_soPvCannotLeakIntoFinancials() {
        // Structural lock: ProjectVariation is non-financial (owner decision 2026-05-25).
        // If someone later injects a ProjectVariationRepository here, this test fails — forcing a design review.
        for (Field f : BoqFinanceDashboardService.class.getDeclaredFields()) {
            assertThat(f.getType().getSimpleName())
                    .as("BoqFinanceDashboardService field %s", f.getName())
                    .doesNotContain("ProjectVariation");
        }
    }

    private void stubZeroInvoicingAndCredits() {
        lenient().when(invoiceRepository.findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of());
        lenient().when(invoiceRepository.sumTotalCollected(anyLong())).thenReturn(BigDecimal.ZERO);
        lenient().when(invoiceRepository.sumOutstandingStageInvoices(anyLong())).thenReturn(BigDecimal.ZERO);
        lenient().when(creditNoteRepository.sumAvailableCredit(anyLong())).thenReturn(BigDecimal.ZERO);
        lenient().when(refundNoticeRepository.findByProjectIdOrderByIssuedAtDesc(anyLong())).thenReturn(List.of());
    }
}
