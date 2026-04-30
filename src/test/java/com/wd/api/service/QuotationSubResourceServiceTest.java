package com.wd.api.service;

import com.wd.api.dto.quotation.AssumptionRequest;
import com.wd.api.dto.quotation.ExclusionRequest;
import com.wd.api.dto.quotation.InclusionRequest;
import com.wd.api.dto.quotation.PaymentMilestoneRequest;
import com.wd.api.model.LeadQuotation;
import com.wd.api.model.QuotationAssumption;
import com.wd.api.model.QuotationExclusion;
import com.wd.api.model.QuotationInclusion;
import com.wd.api.model.QuotationPaymentMilestone;
import com.wd.api.repository.LeadQuotationRepository;
import com.wd.api.repository.QuotationAssumptionRepository;
import com.wd.api.repository.QuotationExclusionRepository;
import com.wd.api.repository.QuotationInclusionRepository;
import com.wd.api.repository.QuotationPaymentMilestoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link QuotationSubResourceService}.
 *
 * <p>The four sub-resources share the same shape, so this suite covers each
 * concern once and trusts the structural symmetry to carry the rest:
 *
 * <ul>
 *   <li><b>display-order auto-append</b> — verified on inclusions; the same
 *       helper is applied to exclusions and assumptions.</li>
 *   <li><b>ownership rejection</b> — verified on exclusions; identical guard
 *       on the other three.</li>
 *   <li><b>milestone-number collision</b> — milestones-only concern.</li>
 *   <li><b>amount derivation from parent.finalAmount × pct</b> — milestones
 *       only; this is the "BUDGETARY publishes shape, DETAILED publishes
 *       rupees" boundary.</li>
 *   <li><b>running percentage total</b> — the helper that powers the
 *       Flutter "85% allocated" hint.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class QuotationSubResourceServiceTest {

    @Mock private LeadQuotationRepository quotationRepository;
    @Mock private QuotationInclusionRepository inclusionRepository;
    @Mock private QuotationExclusionRepository exclusionRepository;
    @Mock private QuotationAssumptionRepository assumptionRepository;
    @Mock private QuotationPaymentMilestoneRepository milestoneRepository;

    private QuotationSubResourceService service;

    private LeadQuotation parent;

    @BeforeEach
    void setUp() {
        service = new QuotationSubResourceService(
                quotationRepository,
                inclusionRepository,
                exclusionRepository,
                assumptionRepository,
                milestoneRepository);

        parent = new LeadQuotation();
        ReflectionTestUtils.setField(parent, "id", 42L);
        parent.setQuotationType("DETAILED");
        parent.setFinalAmount(new BigDecimal("1000000.00")); // ₹10,00,000

        lenient().when(quotationRepository.findById(42L)).thenReturn(Optional.of(parent));
    }

    @Test
    void createInclusion_autoAppendsDisplayOrderWhenOmitted() {
        // Two existing inclusions on the parent → next display_order = 2
        when(inclusionRepository.findByQuotationIdOrderByDisplayOrderAsc(42L))
                .thenReturn(List.of(new QuotationInclusion(), new QuotationInclusion()));
        when(inclusionRepository.save(any(QuotationInclusion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        QuotationInclusion saved = service.createInclusion(
                42L,
                new InclusionRequest(null, "Civil", "RCC structure"));

        assertThat(saved.getDisplayOrder()).isEqualTo(2);
        assertThat(saved.getCategory()).isEqualTo("Civil");
        assertThat(saved.getQuotation()).isSameAs(parent);
    }

    @Test
    void createInclusion_respectsExplicitDisplayOrder() {
        when(inclusionRepository.findByQuotationIdOrderByDisplayOrderAsc(42L))
                .thenReturn(List.of(new QuotationInclusion()));
        when(inclusionRepository.save(any(QuotationInclusion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        QuotationInclusion saved = service.createInclusion(
                42L,
                new InclusionRequest(0, "Civil", "Excavation"));

        assertThat(saved.getDisplayOrder()).isZero();
    }

    @Test
    void updateExclusion_rejectsCrossQuoteTampering() {
        // Exclusion exists, but it belongs to a *different* quotation.
        // The service must treat this as not-found rather than 403, so we
        // don't leak which IDs exist.
        LeadQuotation otherQuote = new LeadQuotation();
        ReflectionTestUtils.setField(otherQuote, "id", 99L);

        QuotationExclusion stranger = new QuotationExclusion();
        ReflectionTestUtils.setField(stranger, "id", 7L);
        stranger.setQuotation(otherQuote);

        when(exclusionRepository.findById(7L)).thenReturn(Optional.of(stranger));

        assertThatThrownBy(() -> service.updateExclusion(
                42L, 7L,
                new ExclusionRequest(null, "Borewell", "₹15,000–25,000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to quotation 42");

        verify(exclusionRepository, never()).save(any());
    }

    @Test
    void createMilestone_rejectsDuplicateMilestoneNumber() {
        QuotationPaymentMilestone existing = new QuotationPaymentMilestone();
        existing.setMilestoneNumber(3);

        when(milestoneRepository.findByQuotationIdOrderByMilestoneNumberAsc(42L))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.createMilestone(
                42L,
                new PaymentMilestoneRequest(3, "Plinth complete",
                        new BigDecimal("15.00"), null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Milestone number 3 already exists");

        verify(milestoneRepository, never()).save(any());
    }

    @Test
    void createMilestone_derivesAmountFromParentFinalAmountWhenNotSupplied() {
        // Parent.finalAmount = 10,00,000; pct = 15% → expected amount = 1,50,000
        when(milestoneRepository.findByQuotationIdOrderByMilestoneNumberAsc(42L))
                .thenReturn(new ArrayList<>());
        when(milestoneRepository.save(any(QuotationPaymentMilestone.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        QuotationPaymentMilestone saved = service.createMilestone(
                42L,
                new PaymentMilestoneRequest(2, "Plinth complete",
                        new BigDecimal("15.00"), null, null));

        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("150000.00"));
    }

    @Test
    void createMilestone_keepsAmountNullWhenParentIsBudgetaryWithoutTotal() {
        // BUDGETARY parent has no finalAmount — the milestone must keep
        // amount null (only percentage is meaningful at the budgetary stage).
        parent.setFinalAmount(null);
        parent.setQuotationType("BUDGETARY");

        when(milestoneRepository.findByQuotationIdOrderByMilestoneNumberAsc(42L))
                .thenReturn(new ArrayList<>());
        when(milestoneRepository.save(any(QuotationPaymentMilestone.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        QuotationPaymentMilestone saved = service.createMilestone(
                42L,
                new PaymentMilestoneRequest(1, "On agreement",
                        new BigDecimal("10.00"), null, null));

        assertThat(saved.getAmount()).isNull();
        assertThat(saved.getPercentage()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void totalMilestonePercentage_sumsAllRows() {
        QuotationPaymentMilestone m1 = new QuotationPaymentMilestone();
        m1.setPercentage(new BigDecimal("10.00"));
        QuotationPaymentMilestone m2 = new QuotationPaymentMilestone();
        m2.setPercentage(new BigDecimal("15.00"));
        QuotationPaymentMilestone m3 = new QuotationPaymentMilestone();
        m3.setPercentage(new BigDecimal("15.00"));

        when(milestoneRepository.findByQuotationIdOrderByMilestoneNumberAsc(42L))
                .thenReturn(List.of(m1, m2, m3));

        assertThat(service.totalMilestonePercentage(42L))
                .isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    void createAssumption_rejectsUnknownQuotationId() {
        when(quotationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAssumption(
                404L,
                new AssumptionRequest(null, "Plot is levelled")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quotation not found: 404");

        verify(assumptionRepository, never()).save(any());
    }

    @Test
    void deleteAssumption_succeedsWhenOwnershipMatches() {
        QuotationAssumption a = new QuotationAssumption();
        ReflectionTestUtils.setField(a, "id", 11L);
        a.setQuotation(parent);

        when(assumptionRepository.findById(11L)).thenReturn(Optional.of(a));

        service.deleteAssumption(42L, 11L);

        verify(assumptionRepository).delete(a);
    }
}
