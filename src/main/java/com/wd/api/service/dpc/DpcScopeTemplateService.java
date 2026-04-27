package com.wd.api.service.dpc;

import com.wd.api.dto.dpc.CreateScopeOptionRequest;
import com.wd.api.dto.dpc.DpcScopeOptionDto;
import com.wd.api.dto.dpc.DpcScopeTemplateDto;
import com.wd.api.dto.dpc.UpdateScopeOptionRequest;
import com.wd.api.dto.dpc.UpdateScopeTemplateRequest;
import com.wd.api.model.DpcScopeOption;
import com.wd.api.model.DpcScopeTemplate;
import com.wd.api.repository.DpcScopeOptionRepository;
import com.wd.api.repository.DpcScopeTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Read + admin edit for the company-level DPC scope-template library.
 *
 * Templates are content-only — they hold the narrative for each scope page
 * (intro, what-you-get, quality procedures, options) plus the BoQ category
 * matchers used by {@link DpcCostRollupService}.  No project-level state.
 */
@Service
@Transactional
public class DpcScopeTemplateService {

    private static final Logger log = LoggerFactory.getLogger(DpcScopeTemplateService.class);

    private final DpcScopeTemplateRepository scopeTemplateRepository;
    private final DpcScopeOptionRepository scopeOptionRepository;

    public DpcScopeTemplateService(DpcScopeTemplateRepository scopeTemplateRepository,
                                   DpcScopeOptionRepository scopeOptionRepository) {
        this.scopeTemplateRepository = scopeTemplateRepository;
        this.scopeOptionRepository = scopeOptionRepository;
    }

    @Transactional(readOnly = true)
    public List<DpcScopeTemplateDto> listAll() {
        List<DpcScopeTemplate> templates = scopeTemplateRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        List<DpcScopeTemplateDto> dtos = new ArrayList<>(templates.size());
        for (DpcScopeTemplate t : templates) {
            List<DpcScopeOption> options =
                    scopeOptionRepository.findByScopeTemplateIdOrderByDisplayOrderAsc(t.getId());
            dtos.add(DpcScopeTemplateDto.from(t, options));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public DpcScopeTemplateDto getById(Long id) {
        DpcScopeTemplate t = scopeTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scope template not found: " + id));
        List<DpcScopeOption> options = scopeOptionRepository.findByScopeTemplateIdOrderByDisplayOrderAsc(id);
        return DpcScopeTemplateDto.from(t, options);
    }

    /**
     * Patch an existing template.  Each non-null field replaces the
     * corresponding entity value; collection fields are replaced wholesale —
     * partial-list editing is not supported here.
     */
    public DpcScopeTemplateDto update(Long id, UpdateScopeTemplateRequest request) {
        DpcScopeTemplate t = scopeTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scope template not found: " + id));

        if (request.title() != null) t.setTitle(request.title());
        if (request.subtitle() != null) t.setSubtitle(request.subtitle());
        if (request.introParagraph() != null) t.setIntroParagraph(request.introParagraph());
        if (request.whatYouGet() != null) t.setWhatYouGet(new ArrayList<>(request.whatYouGet()));
        if (request.qualityProcedures() != null) t.setQualityProcedures(new ArrayList<>(request.qualityProcedures()));
        if (request.documentsYouGet() != null) t.setDocumentsYouGet(new ArrayList<>(request.documentsYouGet()));
        if (request.boqCategoryPatterns() != null) {
            t.setBoqCategoryPatterns(new ArrayList<>(request.boqCategoryPatterns()));
        }
        if (request.defaultBrands() != null) t.setDefaultBrands(new HashMap<>(request.defaultBrands()));
        if (request.isActive() != null) t.setIsActive(request.isActive());

        scopeTemplateRepository.save(t);
        log.info("Updated DPC scope template id={} code={}", t.getId(), t.getCode());

        List<DpcScopeOption> options = scopeOptionRepository.findByScopeTemplateIdOrderByDisplayOrderAsc(id);
        return DpcScopeTemplateDto.from(t, options);
    }

    // ---- Option CRUD ------------------------------------------------------

    /**
     * Append a new option card to a scope template. When {@code displayOrder}
     * is null on the request, the new row gets {@code (max existing) + 1}.
     */
    public DpcScopeOptionDto addOption(Long scopeTemplateId, CreateScopeOptionRequest req) {
        DpcScopeTemplate template = scopeTemplateRepository.findById(scopeTemplateId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Scope template not found: " + scopeTemplateId));

        // Enforce uniqueness of code within this scope (DB UNIQUE handles the
        // race; we surface the friendly error pre-flight).
        List<DpcScopeOption> existing =
                scopeOptionRepository.findByScopeTemplateIdOrderByDisplayOrderAsc(scopeTemplateId);
        for (DpcScopeOption o : existing) {
            if (o.getCode() != null && o.getCode().equalsIgnoreCase(req.code())) {
                throw new IllegalStateException(
                        "Option with code '" + req.code() + "' already exists for this scope");
            }
        }

        int nextOrder = req.displayOrder() != null
                ? req.displayOrder()
                : (existing.isEmpty()
                        ? 1
                        : existing.stream().mapToInt(o ->
                                o.getDisplayOrder() != null ? o.getDisplayOrder() : 0)
                                .max().orElse(0) + 1);

        DpcScopeOption option = new DpcScopeOption();
        option.setScopeTemplate(template);
        option.setCode(req.code());
        option.setDisplayName(req.displayName());
        option.setImagePath(req.imagePath());
        option.setDisplayOrder(nextOrder);
        option.setIsActive(true);
        DpcScopeOption saved = scopeOptionRepository.save(option);

        log.info("Added option id={} code={} to scope {}", saved.getId(), saved.getCode(), scopeTemplateId);
        return DpcScopeOptionDto.from(saved);
    }

    /** Patch an existing option. Code uniqueness is verified within the scope. */
    public DpcScopeOptionDto updateOption(Long optionId, UpdateScopeOptionRequest req) {
        DpcScopeOption option = scopeOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Scope option not found: " + optionId));

        if (req.code() != null && !req.code().equalsIgnoreCase(option.getCode())) {
            Long scopeId = option.getScopeTemplate() != null ? option.getScopeTemplate().getId() : null;
            if (scopeId != null) {
                List<DpcScopeOption> siblings =
                        scopeOptionRepository.findByScopeTemplateIdOrderByDisplayOrderAsc(scopeId);
                for (DpcScopeOption sib : siblings) {
                    if (!sib.getId().equals(optionId)
                            && sib.getCode() != null
                            && sib.getCode().equalsIgnoreCase(req.code())) {
                        throw new IllegalStateException(
                                "Option with code '" + req.code() + "' already exists for this scope");
                    }
                }
            }
            option.setCode(req.code());
        }
        if (req.displayName() != null) option.setDisplayName(req.displayName());
        if (req.imagePath() != null) option.setImagePath(req.imagePath());
        if (req.displayOrder() != null) option.setDisplayOrder(req.displayOrder());
        if (req.isActive() != null) option.setIsActive(req.isActive());

        return DpcScopeOptionDto.from(scopeOptionRepository.save(option));
    }

    /**
     * Soft-delete via the entity's {@code @SQLDelete} — the row gets
     * {@code deleted_at = NOW()} and is hidden from subsequent queries by the
     * {@code @SQLRestriction} filter.
     */
    public void softDeleteOption(Long optionId) {
        DpcScopeOption option = scopeOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Scope option not found: " + optionId));
        scopeOptionRepository.delete(option);
        log.info("Soft-deleted scope option id={} code={}", optionId, option.getCode());
    }

    /**
     * Reassign {@code displayOrder} per the position in the supplied list
     * (1-indexed). The list MUST contain exactly the active option ids of the
     * scope — no missing, no extra — otherwise the call is rejected to avoid
     * silent data drift.
     */
    public List<DpcScopeOptionDto> reorderOptions(Long scopeTemplateId, List<Long> orderedOptionIds) {
        if (!scopeTemplateRepository.existsById(scopeTemplateId)) {
            throw new IllegalArgumentException("Scope template not found: " + scopeTemplateId);
        }
        if (orderedOptionIds == null || orderedOptionIds.isEmpty()) {
            throw new IllegalArgumentException("orderedOptionIds is required");
        }

        List<DpcScopeOption> active =
                scopeOptionRepository.findByScopeTemplateIdOrderByDisplayOrderAsc(scopeTemplateId);
        Set<Long> activeIds = new HashSet<>();
        for (DpcScopeOption o : active) activeIds.add(o.getId());
        Set<Long> requested = new HashSet<>(orderedOptionIds);

        if (!activeIds.equals(requested)) {
            throw new IllegalStateException(
                    "orderedOptionIds must contain exactly the active options for scope "
                            + scopeTemplateId + " (expected " + activeIds + ", got " + requested + ")");
        }

        // Index existing options by id for quick lookup.
        java.util.Map<Long, DpcScopeOption> byId = new java.util.HashMap<>();
        for (DpcScopeOption o : active) byId.put(o.getId(), o);

        List<DpcScopeOption> reordered = new ArrayList<>(orderedOptionIds.size());
        int pos = 1;
        for (Long oid : orderedOptionIds) {
            DpcScopeOption o = byId.get(oid);
            o.setDisplayOrder(pos++);
            reordered.add(o);
        }
        scopeOptionRepository.saveAll(reordered);

        List<DpcScopeOptionDto> dtos = new ArrayList<>(reordered.size());
        for (DpcScopeOption o : reordered) dtos.add(DpcScopeOptionDto.from(o));
        return dtos;
    }
}
