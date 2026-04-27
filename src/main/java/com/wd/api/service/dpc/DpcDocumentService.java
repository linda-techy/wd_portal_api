package com.wd.api.service.dpc;

import com.wd.api.config.CompanyInfoConfig;
import com.wd.api.dto.dpc.DpcCostRollupDto;
import com.wd.api.dto.dpc.DpcCustomizationLineDto;
import com.wd.api.dto.dpc.DpcDocumentDto;
import com.wd.api.dto.dpc.DpcDocumentScopeDto;
import com.wd.api.dto.dpc.DpcMasterCostSummaryDto;
import com.wd.api.dto.dpc.DpcPaymentMilestoneDto;
import com.wd.api.dto.dpc.UpdateDpcDocumentRequest;
import com.wd.api.dto.dpc.UpdateDpcScopeRequest;
import com.wd.api.model.BoqDocument;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.CustomerUser;
import com.wd.api.model.DpcCustomizationLine;
import com.wd.api.model.DpcDocument;
import com.wd.api.model.DpcDocumentScope;
import com.wd.api.model.DpcScopeOption;
import com.wd.api.model.DpcScopeTemplate;
import com.wd.api.model.PaymentStage;
import com.wd.api.model.enums.BoqDocumentStatus;
import com.wd.api.model.enums.DpcCustomizationSource;
import com.wd.api.model.enums.DpcDocumentStatus;
import com.wd.api.repository.BoqDocumentRepository;
import com.wd.api.repository.BoqItemRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.DpcCustomizationLineRepository;
import com.wd.api.repository.DpcDocumentRepository;
import com.wd.api.repository.DpcDocumentScopeRepository;
import com.wd.api.repository.DpcScopeOptionRepository;
import com.wd.api.repository.DpcScopeTemplateRepository;
import com.wd.api.repository.PaymentStageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Top-level DPC document orchestrator.
 *
 * Owns the DPC lifecycle: create from APPROVED BoQ, header / scope edits,
 * issue (locking), and revision branching.  Cost numbers are not stored —
 * {@link DpcCostRollupService} computes them live from the BoQ for every
 * read, which keeps the document a pure render layer.
 */
@Service
@Transactional
public class DpcDocumentService {

    private static final Logger log = LoggerFactory.getLogger(DpcDocumentService.class);

    private final DpcDocumentRepository dpcDocumentRepository;
    private final DpcDocumentScopeRepository dpcDocumentScopeRepository;
    private final DpcCustomizationLineRepository dpcCustomizationLineRepository;
    private final DpcScopeTemplateRepository dpcScopeTemplateRepository;
    private final DpcScopeOptionRepository dpcScopeOptionRepository;
    private final BoqDocumentRepository boqDocumentRepository;
    private final CustomerProjectRepository customerProjectRepository;
    private final BoqItemRepository boqItemRepository;
    private final PaymentStageRepository paymentStageRepository;
    private final DpcCostRollupService costRollupService;
    private final DpcCustomizationService customizationService;
    private final CompanyInfoConfig companyInfoConfig;

    public DpcDocumentService(DpcDocumentRepository dpcDocumentRepository,
                              DpcDocumentScopeRepository dpcDocumentScopeRepository,
                              DpcCustomizationLineRepository dpcCustomizationLineRepository,
                              DpcScopeTemplateRepository dpcScopeTemplateRepository,
                              DpcScopeOptionRepository dpcScopeOptionRepository,
                              BoqDocumentRepository boqDocumentRepository,
                              CustomerProjectRepository customerProjectRepository,
                              BoqItemRepository boqItemRepository,
                              PaymentStageRepository paymentStageRepository,
                              DpcCostRollupService costRollupService,
                              DpcCustomizationService customizationService,
                              CompanyInfoConfig companyInfoConfig) {
        this.dpcDocumentRepository = dpcDocumentRepository;
        this.dpcDocumentScopeRepository = dpcDocumentScopeRepository;
        this.dpcCustomizationLineRepository = dpcCustomizationLineRepository;
        this.dpcScopeTemplateRepository = dpcScopeTemplateRepository;
        this.dpcScopeOptionRepository = dpcScopeOptionRepository;
        this.boqDocumentRepository = boqDocumentRepository;
        this.customerProjectRepository = customerProjectRepository;
        this.boqItemRepository = boqItemRepository;
        this.paymentStageRepository = paymentStageRepository;
        this.costRollupService = costRollupService;
        this.customizationService = customizationService;
        this.companyInfoConfig = companyInfoConfig;
    }

    // ---- Create / Read ----

    /**
     * Create a fresh DRAFT DPC for a project, anchored on the latest APPROVED
     * BoQ.  Seeds one scope row per active template and auto-populates the
     * customization lines from the current ADDON BoQ items.
     */
    public DpcDocumentDto create(Long projectId, Long currentUserId) {
        CustomerProject project = customerProjectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        BoqDocument boq = boqDocumentRepository
                .findFirstByProject_IdAndStatusOrderByApprovedAtDesc(projectId, BoqDocumentStatus.APPROVED)
                .orElseThrow(() -> new IllegalStateException("Project has no APPROVED BoQ"));

        int nextRevision = dpcDocumentRepository.findMaxRevisionByProjectId(projectId).orElse(0) + 1;

        DpcDocument dpc = new DpcDocument();
        dpc.setProject(project);
        dpc.setBoqDocument(boq);
        dpc.setRevisionNumber(nextRevision);
        dpc.setStatus(DpcDocumentStatus.DRAFT);
        applyDefaultContacts(dpc, project);
        dpc = dpcDocumentRepository.save(dpc);

        // Seed scope rows: one per active template, in displayOrder.
        List<DpcScopeTemplate> templates = dpcScopeTemplateRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        List<DpcDocumentScope> seededScopes = new ArrayList<>(templates.size());
        for (DpcScopeTemplate template : templates) {
            DpcDocumentScope scope = new DpcDocumentScope();
            scope.setDpcDocument(dpc);
            scope.setScopeTemplate(template);
            scope.setIncludedInPdf(true);
            scope.setDisplayOrder(template.getDisplayOrder());
            seededScopes.add(scope);
        }
        dpcDocumentScopeRepository.saveAll(seededScopes);

        // Auto-populate customization rows from current ADDON BoQ items.
        customizationService.autoPopulateFromBoq(dpc);

        log.info("Created DPC {} (rev {}) for project {} on BoQ {} by user {}",
                dpc.getId(), nextRevision, projectId, boq.getId(), currentUserId);
        return getById(dpc.getId());
    }

    @Transactional(readOnly = true)
    public DpcDocumentDto getById(Long id) {
        DpcDocument dpc = dpcDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DPC document not found: " + id));
        return assemble(dpc);
    }

    @Transactional(readOnly = true)
    public DpcDocumentDto getLatest(Long projectId) {
        DpcDocument dpc = dpcDocumentRepository.findFirstByProjectIdOrderByRevisionNumberDesc(projectId)
                .orElseThrow(() -> new IllegalArgumentException("No DPC document for project: " + projectId));
        return assemble(dpc);
    }

    // ---- Updates ----

    public DpcDocumentDto updateHeader(Long id, UpdateDpcDocumentRequest req) {
        DpcDocument dpc = dpcDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DPC document not found: " + id));
        assertNotLocked(dpc);

        if (req.titleOverride() != null) dpc.setTitleOverride(req.titleOverride());
        if (req.subtitleOverride() != null) dpc.setSubtitleOverride(req.subtitleOverride());
        if (req.clientSignatoryName() != null) dpc.setClientSignatoryName(req.clientSignatoryName());
        if (req.walldotSignatoryName() != null) dpc.setWalldotSignatoryName(req.walldotSignatoryName());
        if (req.projectEngineerUserId() != null) dpc.setProjectEngineerUserId(req.projectEngineerUserId());
        if (req.branchManagerName() != null) dpc.setBranchManagerName(req.branchManagerName());
        if (req.branchManagerPhone() != null) dpc.setBranchManagerPhone(req.branchManagerPhone());
        if (req.crmTeamName() != null) dpc.setCrmTeamName(req.crmTeamName());
        if (req.crmTeamPhone() != null) dpc.setCrmTeamPhone(req.crmTeamPhone());

        dpcDocumentRepository.save(dpc);
        return assemble(dpc);
    }

    public DpcDocumentDto updateScope(Long dpcId, Long scopeRowId, UpdateDpcScopeRequest req) {
        DpcDocumentScope scope = dpcDocumentScopeRepository.findById(scopeRowId)
                .orElseThrow(() -> new IllegalArgumentException("DPC scope row not found: " + scopeRowId));

        if (scope.getDpcDocument() == null || !dpcId.equals(scope.getDpcDocument().getId())) {
            throw new IllegalArgumentException("Scope row " + scopeRowId + " does not belong to DPC " + dpcId);
        }
        assertNotLocked(scope.getDpcDocument());

        if (req.selectedOptionId() != null) {
            DpcScopeOption option = dpcScopeOptionRepository.findById(req.selectedOptionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Scope option not found: " + req.selectedOptionId()));
            scope.setSelectedOption(option);
        }
        if (req.selectedOptionRationale() != null) scope.setSelectedOptionRationale(req.selectedOptionRationale());
        if (req.brandsOverride() != null) scope.setBrandsOverride(req.brandsOverride());
        if (req.whatYouGetOverride() != null) scope.setWhatYouGetOverride(req.whatYouGetOverride());
        if (req.includedInPdf() != null) scope.setIncludedInPdf(req.includedInPdf());

        dpcDocumentScopeRepository.save(scope);
        return assemble(scope.getDpcDocument());
    }

    // ---- Issue + Revision ----

    /**
     * Stamp the DPC as ISSUED and snapshot the rendered PDF.  Once issued the
     * document becomes read-only — further changes require a new revision.
     */
    public DpcDocumentDto issue(Long id, Long currentUserId, byte[] renderedPdfBytes, Long persistedDocumentId) {
        DpcDocument dpc = dpcDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DPC document not found: " + id));
        if (dpc.isLocked()) {
            throw new IllegalStateException("DPC " + id + " is already ISSUED");
        }

        dpc.setStatus(DpcDocumentStatus.ISSUED);
        dpc.setIssuedAt(LocalDateTime.now());
        dpc.setIssuedByUserId(currentUserId);
        dpc.setIssuedPdfDocumentId(persistedDocumentId);
        dpcDocumentRepository.save(dpc);

        log.info("Issued DPC {} (rev {}) by user {} — pdfDocId={} ({} bytes)",
                dpc.getId(), dpc.getRevisionNumber(), currentUserId, persistedDocumentId,
                renderedPdfBytes != null ? renderedPdfBytes.length : 0);
        return assemble(dpc);
    }

    /**
     * Branch a new DRAFT revision off an ISSUED DPC.  Header / contacts and
     * scope choices are copied forward; the customization lines are refreshed
     * from the current BoQ ADDONs and any MANUAL lines on the previous
     * revision are carried forward unchanged.
     */
    public DpcDocumentDto createNewRevision(Long previousId, Long currentUserId) {
        DpcDocument prev = dpcDocumentRepository.findById(previousId)
                .orElseThrow(() -> new IllegalArgumentException("DPC document not found: " + previousId));

        if (!prev.isIssued()) {
            throw new IllegalStateException(
                    "Cannot branch a new revision from a DRAFT DPC — issue it first");
        }

        DpcDocument next = new DpcDocument();
        next.setProject(prev.getProject());
        next.setBoqDocument(prev.getBoqDocument());
        next.setRevisionNumber(prev.getRevisionNumber() + 1);
        next.setStatus(DpcDocumentStatus.DRAFT);
        next.setTitleOverride(prev.getTitleOverride());
        next.setSubtitleOverride(prev.getSubtitleOverride());
        next.setClientSignatoryName(prev.getClientSignatoryName());
        next.setWalldotSignatoryName(prev.getWalldotSignatoryName());
        next.setProjectEngineerUserId(prev.getProjectEngineerUserId());
        next.setBranchManagerName(prev.getBranchManagerName());
        next.setBranchManagerPhone(prev.getBranchManagerPhone());
        next.setCrmTeamName(prev.getCrmTeamName());
        next.setCrmTeamPhone(prev.getCrmTeamPhone());
        next = dpcDocumentRepository.save(next);

        // Copy scope rows (preserves customer's selected options + rationales).
        List<DpcDocumentScope> prevScopes =
                dpcDocumentScopeRepository.findByDpcDocumentIdOrderByDisplayOrderAsc(previousId);
        List<DpcDocumentScope> copies = new ArrayList<>(prevScopes.size());
        for (DpcDocumentScope src : prevScopes) {
            DpcDocumentScope copy = new DpcDocumentScope();
            copy.setDpcDocument(next);
            copy.setScopeTemplate(src.getScopeTemplate());
            copy.setSelectedOption(src.getSelectedOption());
            copy.setSelectedOptionRationale(src.getSelectedOptionRationale());
            copy.setBrandsOverride(src.getBrandsOverride() != null ? new HashMap<>(src.getBrandsOverride()) : null);
            copy.setWhatYouGetOverride(src.getWhatYouGetOverride() != null
                    ? new ArrayList<>(src.getWhatYouGetOverride()) : null);
            copy.setIncludedInPdf(src.getIncludedInPdf());
            copy.setDisplayOrder(src.getDisplayOrder());
            copies.add(copy);
        }
        dpcDocumentScopeRepository.saveAll(copies);

        // Refresh AUTO customization rows from the current BoQ ADDONs.
        customizationService.autoPopulateFromBoq(next);

        // Carry forward MANUAL lines from the previous revision.
        List<DpcCustomizationLine> prevLines =
                dpcCustomizationLineRepository.findByDpcDocumentIdOrderByDisplayOrderAsc(previousId);
        List<DpcCustomizationLine> manualCopies = new ArrayList<>();
        int autoCount = (int) prevLines.stream()
                .filter(l -> DpcCustomizationSource.AUTO_FROM_BOQ_ADDON == l.getSource()).count();
        int order = autoCount;
        for (DpcCustomizationLine src : prevLines) {
            if (DpcCustomizationSource.MANUAL != src.getSource()) continue;
            DpcCustomizationLine copy = new DpcCustomizationLine();
            copy.setDpcDocument(next);
            copy.setSource(DpcCustomizationSource.MANUAL);
            copy.setTitle(src.getTitle());
            copy.setDescription(src.getDescription());
            copy.setAmount(src.getAmount());
            copy.setBoqItemId(src.getBoqItemId());
            copy.setDisplayOrder(order++);
            manualCopies.add(copy);
        }
        if (!manualCopies.isEmpty()) {
            dpcCustomizationLineRepository.saveAll(manualCopies);
        }

        log.info("Branched DPC {} rev {} -> new DPC {} rev {} by user {}",
                prev.getId(), prev.getRevisionNumber(), next.getId(), next.getRevisionNumber(), currentUserId);
        return getById(next.getId());
    }

    // ---- Helpers ----

    private DpcDocumentDto assemble(DpcDocument dpc) {
        Long dpcId = dpc.getId();
        Long projectId = dpc.getProject() != null ? dpc.getProject().getId() : null;
        Long boqDocumentId = dpc.getBoqDocument() != null ? dpc.getBoqDocument().getId() : null;

        // Cost rollup (and per-scope amounts indexed by template id for quick lookup).
        DpcMasterCostSummaryDto summary = (projectId != null && boqDocumentId != null)
                ? costRollupService.computeForProject(projectId, boqDocumentId)
                : null;
        Map<String, DpcCostRollupDto> rollupByCode = new HashMap<>();
        if (summary != null) {
            for (DpcCostRollupDto r : summary.scopes()) rollupByCode.put(r.scopeCode(), r);
        }

        // Scopes -> DTOs (with options + rollup amounts resolved).
        List<DpcDocumentScope> scopes =
                dpcDocumentScopeRepository.findByDpcDocumentIdOrderByDisplayOrderAsc(dpcId);
        List<DpcDocumentScopeDto> scopeDtos = new ArrayList<>(scopes.size());
        for (DpcDocumentScope scope : scopes) {
            DpcScopeTemplate template = scope.getScopeTemplate();
            List<DpcScopeOption> options = template != null
                    ? dpcScopeOptionRepository.findByScopeTemplateIdOrderByDisplayOrderAsc(template.getId())
                    : List.of();
            DpcCostRollupDto rollup = template != null ? rollupByCode.get(template.getCode()) : null;
            scopeDtos.add(DpcDocumentScopeDto.from(scope, template, options, rollup));
        }

        // Customizations.
        List<DpcCustomizationLine> lines =
                dpcCustomizationLineRepository.findByDpcDocumentIdOrderByDisplayOrderAsc(dpcId);
        List<DpcCustomizationLineDto> lineDtos = new ArrayList<>(lines.size());
        for (DpcCustomizationLine line : lines) lineDtos.add(DpcCustomizationLineDto.from(line));

        // Payment milestones — sourced from PaymentStage rows, not the design-package PaymentSchedule.
        List<DpcPaymentMilestoneDto> milestones = List.of();
        if (boqDocumentId != null) {
            List<PaymentStage> stages =
                    paymentStageRepository.findByBoqDocumentIdOrderByStageNumberAsc(boqDocumentId);
            milestones = DpcPaymentMilestoneDto.fromStages(stages);
        }

        return DpcDocumentDto.from(dpc, dpc.getProject(), scopeDtos, lineDtos, summary, milestones);
    }

    private void applyDefaultContacts(DpcDocument dpc, CustomerProject project) {
        // Walldot defaults pulled from CompanyInfoConfig at create time; editor
        // can patch them via updateHeader before issue.
        String companyName = companyInfoConfig.getName();
        String companyPhone = companyInfoConfig.getPhone();

        if (dpc.getBranchManagerName() == null) dpc.setBranchManagerName(companyName + " Branch");
        if (dpc.getBranchManagerPhone() == null) dpc.setBranchManagerPhone(companyPhone);
        if (dpc.getCrmTeamName() == null) dpc.setCrmTeamName("Customer Relations");
        if (dpc.getCrmTeamPhone() == null) dpc.setCrmTeamPhone(companyPhone);

        if (dpc.getClientSignatoryName() == null) {
            CustomerUser customer = project != null ? project.getCustomer() : null;
            if (customer != null) {
                String first = customer.getFirstName() != null ? customer.getFirstName() : "";
                String last = customer.getLastName() != null ? customer.getLastName() : "";
                String fullName = (first + " " + last).trim();
                dpc.setClientSignatoryName(fullName.isEmpty() ? (project != null ? project.getName() : null) : fullName);
            } else if (project != null) {
                dpc.setClientSignatoryName(project.getName());
            }
        }
    }

    private void assertNotLocked(DpcDocument dpc) {
        if (dpc.isLocked()) {
            throw new IllegalStateException("DPC " + dpc.getId() + " is ISSUED and cannot be modified");
        }
    }
}
