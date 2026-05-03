package com.wd.api.estimation.service;

import com.wd.api.estimation.domain.*;
import com.wd.api.estimation.domain.enums.EstimationStatus;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.dto.*;
import com.wd.api.model.Lead;
import com.wd.api.repository.LeadRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class LeadEstimationServiceTest extends TestcontainersPostgresBase {

    @Autowired private EntityManager em;
    @Autowired private LeadEstimationService service;
    @Autowired private LeadRepository leadRepo;

    private static final Long LEAD_ID = 1L;

    private UUID packageId;

    @BeforeEach
    void seed() {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("Signature");
        em.persist(pkg);

        PackageRateVersion rv = new PackageRateVersion();
        rv.setPackageId(pkg.getId());
        rv.setProjectType(ProjectType.NEW_BUILD);
        rv.setMaterialRate(new BigDecimal("1500.00"));
        rv.setLabourRate(new BigDecimal("550.00"));
        rv.setOverheadRate(new BigDecimal("300.00"));
        rv.setEffectiveFrom(LocalDate.of(2026, 4, 1));
        em.persist(rv);

        MarketIndexSnapshot mi = new MarketIndexSnapshot();
        mi.setSnapshotDate(LocalDate.now());
        mi.setSteelRate(new BigDecimal("62.50"));
        mi.setCementRate(new BigDecimal("410.00"));
        mi.setSandRate(new BigDecimal("5800.00"));
        mi.setAggregateRate(new BigDecimal("1850.00"));
        mi.setTilesRate(new BigDecimal("38.00"));
        mi.setElectricalRate(new BigDecimal("92.00"));
        mi.setPaintsRate(new BigDecimal("285.00"));
        mi.setWeightsJson(Map.of("steel", "0.30"));
        mi.setCompositeIndex(new BigDecimal("1.0000"));
        mi.setActive(true);
        em.persist(mi);
        em.flush();

        packageId = pkg.getId();
    }

    private LeadEstimationCreateRequest standardRequest() {
        DimensionsDto dim = new DimensionsDto(
                List.of(new FloorDto("Ground", new BigDecimal("30"), new BigDecimal("35"))),
                BigDecimal.ZERO, BigDecimal.ZERO);
        CalculatePreviewRequest preview = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD, packageId,
                null, null, dim, List.of(), List.of(), List.of(), List.of(),
                null, null);
        return new LeadEstimationCreateRequest(LEAD_ID, preview, null);
    }

    @Test
    void create_persistsEstimationWithLineItems() {
        LeadEstimationDetailResponse resp = service.create(standardRequest());
        assertThat(resp.estimationNo()).startsWith("EST-");
        assertThat(resp.grandTotal()).isPositive();
        assertThat(resp.lineItems()).isNotEmpty();
        assertThat(resp.status()).isEqualTo(EstimationStatus.DRAFT);
        assertThat(resp.rateVersionId()).isNotNull();
        assertThat(resp.marketIndexId()).isNotNull();
        // Sub-resource lists are present (empty on a freshly created estimation)
        assertThat(resp.inclusions()).isNotNull().isEmpty();
        assertThat(resp.exclusions()).isNotNull().isEmpty();
        assertThat(resp.assumptions()).isNotNull().isEmpty();
        assertThat(resp.paymentMilestones()).isNotNull().isEmpty();
    }

    @Test
    void listByLead_returnsNewestFirst() {
        service.create(standardRequest());
        service.create(standardRequest());

        List<LeadEstimationSummaryResponse> items = service.listByLead(LEAD_ID);
        assertThat(items).hasSize(2);
        assertThat(items.get(0).createdAt()).isAfterOrEqualTo(items.get(1).createdAt());
    }

    @Test
    void delete_softDeletesEstimation() {
        LeadEstimationDetailResponse created = service.create(standardRequest());
        service.delete(created.id());

        List<LeadEstimationSummaryResponse> remaining = service.listByLead(LEAD_ID);
        assertThat(remaining).isEmpty();
    }

    // ── Sub-project F: status transition tests ────────────────────────────────

    @Test
    void markSent_transitions_DRAFT_to_SENT() {
        LeadEstimationDetailResponse created = service.create(standardRequest());
        assertThat(created.status()).isEqualTo(EstimationStatus.DRAFT);

        LeadEstimationDetailResponse sent = service.markSent(created.id());
        assertThat(sent.status()).isEqualTo(EstimationStatus.SENT);
    }

    @Test
    void markSent_throws_when_estimation_is_not_DRAFT() {
        LeadEstimationDetailResponse created = service.create(standardRequest());
        service.markSent(created.id()); // DRAFT → SENT
        // Now it is SENT — markSent again must throw
        UUID id = created.id();
        assertThatThrownBy(() -> service.markSent(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SENT");
    }

    @Test
    void markAccepted_transitions_SENT_to_ACCEPTED_and_flips_lead_status() {
        // Persist a real Lead row so the best-effort hook can find and update it.
        Lead lead = new Lead();
        lead.setLeadStatus("proposal_sent");
        lead.setName("Test Lead");
        Lead savedLead = leadRepo.save(lead);
        em.flush();

        LeadEstimationCreateRequest req = new LeadEstimationCreateRequest(
                savedLead.getId(), standardRequest().preview(), null);
        LeadEstimationDetailResponse created = service.create(req);
        service.markSent(created.id());

        LeadEstimationDetailResponse accepted = service.markAccepted(created.id());
        assertThat(accepted.status()).isEqualTo(EstimationStatus.ACCEPTED);

        // Clear the 1st-level cache so we reload from the session state (updated by service).
        em.flush();
        em.clear();
        Lead reloaded = leadRepo.findById(savedLead.getId()).orElseThrow();
        assertThat(reloaded.getLeadStatus()).isEqualTo("project_won");
    }

    @Test
    void markRejected_transitions_SENT_to_REJECTED() {
        LeadEstimationDetailResponse created = service.create(standardRequest());
        service.markSent(created.id());

        LeadEstimationDetailResponse rejected = service.markRejected(created.id());
        assertThat(rejected.status()).isEqualTo(EstimationStatus.REJECTED);
    }

    @Test
    void revertToDraft_works_for_SENT_and_REJECTED_but_throws_for_ACCEPTED() {
        // SENT → DRAFT (allowed)
        LeadEstimationDetailResponse e1 = service.create(standardRequest());
        service.markSent(e1.id());
        LeadEstimationDetailResponse reverted = service.revertToDraft(e1.id());
        assertThat(reverted.status()).isEqualTo(EstimationStatus.DRAFT);

        // REJECTED → DRAFT (allowed)
        LeadEstimationDetailResponse e2 = service.create(standardRequest());
        service.markSent(e2.id());
        service.markRejected(e2.id());
        LeadEstimationDetailResponse fromRejected = service.revertToDraft(e2.id());
        assertThat(fromRejected.status()).isEqualTo(EstimationStatus.DRAFT);

        // ACCEPTED → DRAFT (forbidden)
        Lead lead = new Lead();
        lead.setLeadStatus("proposal_sent");
        lead.setName("Revert Test Lead");
        Lead savedLead = leadRepo.save(lead);
        em.flush();
        LeadEstimationCreateRequest req = new LeadEstimationCreateRequest(
                savedLead.getId(), standardRequest().preview(), null);
        LeadEstimationDetailResponse e3 = service.create(req);
        service.markSent(e3.id());
        service.markAccepted(e3.id());
        UUID id3 = e3.id();
        assertThatThrownBy(() -> service.revertToDraft(id3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACCEPTED");
    }
}
