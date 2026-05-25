package com.wd.api.service;

import com.wd.api.model.BoqInvoice;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.PaymentStage;
import com.wd.api.model.enums.PaymentStageStatus;
import com.wd.api.repository.BoqInvoiceRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PaymentStageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BoqInvoiceService.raiseStageInvoice.
 *
 * Focus: when a stage invoice is raised the PaymentStage must end up with
 *   - dueDate == the invoice due date
 *   - status  == INVOICED
 * and a duplicate raise on an already-INVOICED stage must be rejected.
 */
@ExtendWith(MockitoExtension.class)
class BoqInvoiceServiceRaiseStageTest {

    @Mock private BoqInvoiceRepository invoiceRepository;
    @Mock private PaymentStageRepository stageRepository;
    @Mock private CustomerProjectRepository projectRepository;

    @InjectMocks
    private BoqInvoiceService invoiceService;

    private CustomerProject project;
    private PaymentStage stage;

    @BeforeEach
    void setUp() {
        project = new CustomerProject();
        project.setId(10L);
        project.setName("Skyline Tower");
        project.setCode("ST10");

        stage = new PaymentStage();
        stage.setId(1L);
        stage.setProject(project);
        stage.setStageNumber(1);
        stage.setStageName("Advance");
        stage.setStatus(PaymentStageStatus.DUE);
        stage.setStageAmountExGst(new BigDecimal("100000.000000"));
        stage.setGstRate(new BigDecimal("0.18"));
        stage.setGstAmount(new BigDecimal("18000.000000"));
        stage.setStageAmountInclGst(new BigDecimal("118000.000000"));
        stage.setBoqValueSnapshot(new BigDecimal("100000.000000"));
        stage.setStagePercentage(new BigDecimal("0.3000"));
        stage.setAppliedCreditAmount(BigDecimal.ZERO);
        stage.setRetentionPct(BigDecimal.ZERO);
        stage.setRetentionHeld(BigDecimal.ZERO);
        stage.setNetPayableAmount(new BigDecimal("118000.000000"));
        stage.setPaidAmount(BigDecimal.ZERO);
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    void raiseStageInvoice_dueStageSetsPaymentStageDueDateAndStatusInvoiced() {
        LocalDate invoiceDueDate = LocalDate.of(2026, 6, 30);

        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage));
        when(invoiceRepository.countByProjectIdAndDeletedAtIsNull(10L)).thenReturn(0L);

        BoqInvoice savedInvoice = new BoqInvoice();
        savedInvoice.setId(99L);
        when(invoiceRepository.save(any(BoqInvoice.class))).thenReturn(savedInvoice);

        ArgumentCaptor<PaymentStage> stageCaptor = ArgumentCaptor.forClass(PaymentStage.class);
        when(stageRepository.save(stageCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        invoiceService.raiseStageInvoice(1L, invoiceDueDate, 7L);

        PaymentStage persisted = stageCaptor.getValue();
        assertThat(persisted.getStatus())
                .as("PaymentStage.status must be INVOICED after raising a stage invoice")
                .isEqualTo(PaymentStageStatus.INVOICED);
        assertThat(persisted.getDueDate())
                .as("PaymentStage.dueDate must equal the invoice due date")
                .isEqualTo(invoiceDueDate);
    }

    @Test
    void raiseStageInvoice_invoiceEntityCarriesDueDate() {
        LocalDate invoiceDueDate = LocalDate.of(2026, 7, 15);

        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage));
        when(invoiceRepository.countByProjectIdAndDeletedAtIsNull(10L)).thenReturn(2L);

        ArgumentCaptor<BoqInvoice> invoiceCaptor = ArgumentCaptor.forClass(BoqInvoice.class);
        BoqInvoice savedInvoice = new BoqInvoice();
        savedInvoice.setId(100L);
        when(invoiceRepository.save(invoiceCaptor.capture())).thenReturn(savedInvoice);
        when(stageRepository.save(any(PaymentStage.class))).thenAnswer(i -> i.getArgument(0));

        invoiceService.raiseStageInvoice(1L, invoiceDueDate, 7L);

        assertThat(invoiceCaptor.getValue().getDueDate())
                .as("BoqInvoice.dueDate must be set to the requested due date")
                .isEqualTo(invoiceDueDate);
    }

    // ── guard: duplicate raise ────────────────────────────────────────────────

    @Test
    void raiseStageInvoice_alreadyInvoicedStage_throwsIllegalStateException() {
        stage.setStatus(PaymentStageStatus.INVOICED);
        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage));

        assertThatThrownBy(() -> invoiceService.raiseStageInvoice(1L, LocalDate.now().plusDays(30), 7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been invoiced");
    }

    @Test
    void raiseStageInvoice_paidStage_throwsIllegalStateException() {
        stage.setStatus(PaymentStageStatus.PAID);
        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage));

        assertThatThrownBy(() -> invoiceService.raiseStageInvoice(1L, LocalDate.now().plusDays(30), 7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been invoiced");
    }

    // ── upstream stage: UPCOMING is also raisable ─────────────────────────────

    @Test
    void raiseStageInvoice_upcomingStageSetsDueDateAndInvoicedStatus() {
        stage.setStatus(PaymentStageStatus.UPCOMING);
        LocalDate invoiceDueDate = LocalDate.of(2026, 8, 1);

        when(stageRepository.findById(1L)).thenReturn(Optional.of(stage));
        when(invoiceRepository.countByProjectIdAndDeletedAtIsNull(10L)).thenReturn(0L);

        BoqInvoice savedInvoice = new BoqInvoice();
        savedInvoice.setId(101L);
        when(invoiceRepository.save(any(BoqInvoice.class))).thenReturn(savedInvoice);

        ArgumentCaptor<PaymentStage> stageCaptor = ArgumentCaptor.forClass(PaymentStage.class);
        when(stageRepository.save(stageCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        invoiceService.raiseStageInvoice(1L, invoiceDueDate, 7L);

        PaymentStage persisted = stageCaptor.getValue();
        assertThat(persisted.getDueDate()).isEqualTo(invoiceDueDate);
        assertThat(persisted.getStatus()).isEqualTo(PaymentStageStatus.INVOICED);
    }
}
